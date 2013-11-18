package org.sagebionetworks.repo.web;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.ChunkedFileToken;
import org.sagebionetworks.repo.model.file.CreateChunkedFileTokenRequest;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.sagebionetworks.repo.util.TempFileProvider;
import org.sagebionetworks.utils.MD5ChecksumHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;

/**
 * Utility for converting between the WikiPage and V2WikiPage models.
 * @author hso
 *
 */
public class WikiModelTranslationHelper implements WikiModelTranslator {
	@Autowired
	FileHandleManager fileHandleManager;
	@Autowired
	FileHandleDao fileMetadataDao;	
	@Autowired
	AmazonS3Client s3Client;
	@Autowired
	TempFileProvider tempFileProvider;
	
	public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";

	public WikiModelTranslationHelper() {}
	
	public WikiModelTranslationHelper(FileHandleManager fileHandleManager, FileHandleDao fileMetadataDao,
			AmazonS3Client s3Client, TempFileProvider tempFileProvider) {
		super();
		this.fileMetadataDao = fileMetadataDao;
		this.fileHandleManager = fileHandleManager;
		this.s3Client = s3Client;
		this.tempFileProvider = tempFileProvider;
	}
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public V2WikiPage convertToV2WikiPage(WikiPage from, UserInfo userInfo) throws IOException {
		if(from == null) throw new IllegalArgumentException("WikiPage cannot be null");
		if(userInfo == null) throw new IllegalArgumentException("User cannot be null");
		V2WikiPage wiki = new V2WikiPage();
		wiki.setId(from.getId());
		wiki.setEtag(from.getEtag());
		wiki.setCreatedOn(from.getCreatedOn());
		wiki.setCreatedBy(from.getCreatedBy());
		wiki.setModifiedBy(from.getModifiedBy());
		wiki.setModifiedOn(from.getModifiedOn());
		wiki.setParentWikiId(from.getParentWikiId());
		wiki.setTitle(from.getTitle());
		wiki.setAttachmentFileHandleIds(from.getAttachmentFileHandleIds());
		
		// Zip up the markdown into a file
		// The upload file will hold the newly created markdown file.
		File markdownTemp = tempFileProvider.createTempFile(wiki.getId()+ "_markdown", ".tmp");
		String markdown = from.getMarkdown();
		if(markdown != null) {
			FileUtils.writeByteArrayToFile(markdownTemp, markdown.getBytes());
		} else {
			// When creating a wiki for the first time, markdown content doesn't exist
			// Uploaded file should be empty
			byte[] emptyByteArray = new byte[0];
			FileUtils.writeByteArrayToFile(markdownTemp, emptyByteArray);
		}
		String contentType = guessContentTypeFromStream(markdownTemp);
		CreateChunkedFileTokenRequest ccftr = new CreateChunkedFileTokenRequest();
		ccftr.setContentType(contentType);
		ccftr.setFileName(markdownTemp.getName());
		// Calculate the MD5
		String md5 = MD5ChecksumHelper.getMD5Checksum(markdownTemp);
		ccftr.setContentMD5(md5);
		// Start the upload
		ChunkedFileToken token = fileHandleManager.createChunkedFileUploadToken(userInfo, ccftr);

		S3FileHandle handle = new S3FileHandle();
		handle.setContentType(token.getContentType());
		handle.setContentMd5(token.getContentMD5());
		handle.setContentSize(markdownTemp.length());
		handle.setFileName(wiki.getId() + "_markdown.txt");
		// Creator of the wiki page may not have been set to the user yet
		// so do not use wiki's createdBy
		handle.setCreatedBy(userInfo.getIndividualGroup().getId());
		long currentTime = System.currentTimeMillis();
		handle.setCreatedOn(new Date(currentTime));
		handle.setKey(token.getKey());
		handle.setBucketName(StackConfiguration.getS3Bucket());
		// Upload this to S3
		s3Client.putObject(StackConfiguration.getS3Bucket(), token.getKey(), markdownTemp);
		// Save the metadata
		handle = fileMetadataDao.createFile(handle);
		
		// Set the file handle id
		wiki.setMarkdownFileHandleId(handle.getId());
		
		if(markdownTemp != null){
			markdownTemp.delete();
		}
		return wiki;
	}
	
	public static String guessContentTypeFromStream(File file)	throws FileNotFoundException, IOException {
		InputStream is = new BufferedInputStream(new FileInputStream(file));
		try{
			// Let java guess from the stream.
			String contentType = URLConnection.guessContentTypeFromStream(is);
			// If Java fails then set the content type to be octet-stream
			if(contentType == null){
				contentType = APPLICATION_OCTET_STREAM;
			}
			return contentType;
		}finally{
			is.close();
		}
	}
	
	@Override
	public WikiPage convertToWikiPage(V2WikiPage from) throws NotFoundException, FileNotFoundException, IOException {
		if(from == null) throw new IllegalArgumentException("WikiPage cannot be null");
		WikiPage wiki = new WikiPage();
		wiki.setId(from.getId());
		wiki.setEtag(from.getEtag());
		wiki.setCreatedOn(from.getCreatedOn());
		wiki.setCreatedBy(from.getCreatedBy());
		wiki.setModifiedBy(from.getModifiedBy());
		wiki.setModifiedOn(from.getModifiedOn());
		wiki.setParentWikiId(from.getParentWikiId());
		wiki.setTitle(from.getTitle());
		wiki.setAttachmentFileHandleIds(from.getAttachmentFileHandleIds());
		
		S3FileHandle markdownHandle = (S3FileHandle) fileMetadataDao.get(from.getMarkdownFileHandleId());
		File markdownTemp = tempFileProvider.createTempFile(wiki.getId()+ "_markdown", ".tmp");
		// Retrieve uploaded markdown
		ObjectMetadata markdownMeta = s3Client.getObject(new GetObjectRequest(markdownHandle.getBucketName(), 
				markdownHandle.getKey()), markdownTemp);
		// Read the file as a string
		String markdownString = FileUtils.readFileToString(markdownTemp, "UTF-8");
		wiki.setMarkdown(markdownString);
		if(markdownTemp != null){
			markdownTemp.delete();
		}
		return wiki;
	}


}
