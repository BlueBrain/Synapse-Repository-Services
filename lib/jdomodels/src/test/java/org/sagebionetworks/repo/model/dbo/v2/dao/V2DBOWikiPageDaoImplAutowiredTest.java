package org.sagebionetworks.repo.model.dbo.v2.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ATTACHMENT_RESERVATION_FILE_HANDLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ATTACHMENT_RESERVATION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_TABLE_WIKI_ATTACHMENT_RESERVATION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ROOT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_TABLE_WIKI_PAGE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ID;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.v2.dao.V2WikiPageDao;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiHeader;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiHistorySnapshot;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class V2DBOWikiPageDaoImplAutowiredTest {

	private static final String SQL_GET_ROOT_ID = "SELECT "+V2_COL_WIKI_ROOT_ID+" FROM "+V2_TABLE_WIKI_PAGE+" WHERE "+V2_COL_WIKI_ID+" = ?";

	@Autowired
	FileHandleDao fileMetadataDao;
	
	@Autowired
	V2WikiPageDao wikiPageDao;
	
	@Autowired
	private UserGroupDAO userGroupDAO;

	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;
	
	private List<WikiPageKey> toDelete;
	String creatorUserGroupId;
	
	S3FileHandle attachOne;
	S3FileHandle attachTwo;
	S3FileHandle markdownOne;
	S3FileHandle markdownTwo;
	
	@After
	public void after(){
		if(wikiPageDao != null && toDelete != null){
			for(WikiPageKey id: toDelete){
				wikiPageDao.delete(id);
			}
		}
		// Delete the file handles
		if(attachOne != null){
			fileMetadataDao.delete(attachOne.getId());
		}
		if(attachTwo != null){
			fileMetadataDao.delete(attachTwo.getId());
		}
		
		if(markdownOne != null) {
			fileMetadataDao.delete(markdownOne.getId());
		}
		
		if(markdownTwo != null) {
			fileMetadataDao.delete(markdownTwo.getId());
		}
	}
	
	@Before
	public void before(){
		toDelete = new LinkedList<WikiPageKey>();
		creatorUserGroupId = userGroupDAO.findGroup(AuthorizationConstants.BOOTSTRAP_USER_GROUP_NAME, false).getId();
		assertNotNull(creatorUserGroupId);
		// We use a long file name to test the uniqueness constraint
		String longFileNamePrefix = "loooooooooooooooooooooooooooooooooooooonnnnnnnnnnnnnnnnnnnnnnnnnnnggggggggggggggggggggggggg";
		
		// Create a few files
		S3FileHandle meta = new S3FileHandle();
		meta.setBucketName("bucketName");
		meta.setKey("key");
		meta.setContentType("content type");
		meta.setContentSize(123l);
		meta.setContentMd5("md5");
		meta.setCreatedBy(creatorUserGroupId);
		meta.setFileName(longFileNamePrefix+".txt1");
		meta = fileMetadataDao.createFile(meta);
		attachOne = meta;

		meta = new S3FileHandle();
		meta.setBucketName("bucketName2");
		meta.setKey("key2");
		meta.setContentType("content type2");
		meta.setContentSize(123l);
		meta.setContentMd5("md52");
		meta.setCreatedBy(creatorUserGroupId);
		meta.setFileName(longFileNamePrefix+".txt2");
		meta = fileMetadataDao.createFile(meta);
		attachTwo = meta;
		
		//Create different markdown content
		meta = new S3FileHandle();
		meta.setBucketName("markdownBucketName");
		meta.setKey("key3");
		meta.setContentType("content type3");
		meta.setContentSize((long) 1231);
		meta.setContentMd5("md53");
		meta.setCreatedBy(creatorUserGroupId);
		meta.setFileName("markdown1");
		markdownOne = fileMetadataDao.createFile(meta);
		
		meta = new S3FileHandle();
		meta.setBucketName("markdownBucketName2");
		meta.setKey("key4");
		meta.setContentType("content type4");
		meta.setContentSize((long) 1231);
		meta.setContentMd5("md54");
		meta.setCreatedBy(creatorUserGroupId);
		meta.setFileName("markdown2");
		markdownTwo = fileMetadataDao.createFile(meta);
	}
	
	/**
	 * Create a new wiki page.
	 * @throws NotFoundException 
	 */
	@Test
	public void testCreate() throws NotFoundException{
		// Create a new wiki page with a single attachment
		V2WikiPage page = new V2WikiPage();
		String ownerId = "syn182";
		ObjectType ownerType = ObjectType.ENTITY;
		page.setTitle("Title");
		page.setCreatedBy(creatorUserGroupId);
		page.setModifiedBy(creatorUserGroupId);
		page.setMarkdownFileHandleId(markdownOne.getId());
		
		// Add an attachment
		page.setAttachmentFileHandleIds(new LinkedList<String>());
		page.getAttachmentFileHandleIds().add(attachOne.getId());
		Map<String, FileHandle> fileNameMap = new HashMap<String, FileHandle>();
		fileNameMap.put(attachOne.getFileName(), attachOne);
		List<String> newIds = new ArrayList<String>();
		newIds.add(attachOne.getId());
		
		// Create it
		V2WikiPage clone = wikiPageDao.create(page, fileNameMap, ownerId, ownerType, newIds);
		assertNotNull(clone);
		assertNotNull(clone.getId());
		
		WikiPageKey key = new WikiPageKey(ownerId, ownerType, clone.getId());
		toDelete.add(key);
		assertNotNull("createdOn date should have been filled in by the DB", clone.getCreatedOn());
		assertNotNull("modifiedOn date should have been filled in by the DB", clone.getModifiedOn());
		assertNotNull(clone.getEtag());
		assertNotNull(clone.getId());
		
		assertEquals(creatorUserGroupId, clone.getCreatedBy());
		assertEquals(creatorUserGroupId, clone.getModifiedBy());
		assertEquals(page.getTitle(), clone.getTitle());
		assertEquals(null, clone.getParentWikiId());
		// Markdown file handle id and attachment ids should not change
		// They are retrieved from the markdown table and attachment reservation table
		assertEquals(page.getMarkdownFileHandleId(), clone.getMarkdownFileHandleId());
		assertEquals(page.getAttachmentFileHandleIds(), clone.getAttachmentFileHandleIds());
		
		// Make sure we can lock
		String etag = wikiPageDao.lockForUpdate(clone.getId());
		assertNotNull(etag);
		assertEquals(clone.getEtag(), etag);
		
		// Make sure the key matches
		WikiPageKey lookupKey = wikiPageDao.lookupWikiKey(key.getWikiPageId());
		assertEquals(key, lookupKey);
	}
	
	/**
	 * Create and update a wiki page. Restore an older version and 
	 * confirm wiki history is accurate
	 * @throws NotFoundException, InterruptedException
	 */
	@Test
	public void testUpdate() throws NotFoundException, InterruptedException{
		V2WikiPage page = new V2WikiPage();
        String ownerId = "syn1082";
        ObjectType ownerType = ObjectType.EVALUATION;
        page.setTitle("Title");
        page.setCreatedBy(creatorUserGroupId);
        page.setModifiedBy(creatorUserGroupId);
        page.setMarkdownFileHandleId(markdownOne.getId());
        
        // Add an attachment
        page.setAttachmentFileHandleIds(new LinkedList<String>());
        page.getAttachmentFileHandleIds().add(attachOne.getId());
        Map<String, FileHandle> fileNameMap = new HashMap<String, FileHandle>();
        fileNameMap.put(attachOne.getFileName(), attachOne);
        List<String> newIds = new ArrayList<String>();
        newIds.add(attachOne.getId());
        
        // Create it
        V2WikiPage clone = wikiPageDao.create(page, fileNameMap, ownerId, ownerType, newIds);
        assertNotNull(clone);

        WikiPageKey key = new WikiPageKey(ownerId, ownerType, clone.getId());
        toDelete.add(key);
        String startEtag = clone.getEtag();
        Long startModifiedOn = clone.getModifiedOn().getTime();
        
        List<Long> reservationIdsBeforeUpdate = wikiPageDao.getFileHandleReservationForWiki(key);
        // The archive should have one entry
        assertTrue(reservationIdsBeforeUpdate.size() == 1);
        
        // Sleep to ensure the next date is higher.
        Thread.sleep(1000);
        
        // Add another attachment to the list and update markdown filehandle id to new markdown
        clone.getAttachmentFileHandleIds().add(attachTwo.getId());
        fileNameMap.put(attachTwo.getFileName(), attachTwo);
        
        List<String> newIds2 = new ArrayList<String>();
        newIds2.add(attachTwo.getId());
        
        // Update
        V2WikiPage clone2 = wikiPageDao.updateWikiPage(clone, fileNameMap, ownerId, ownerType, newIds2);                
        assertNotNull(clone2);
        assertNotNull(clone2.getEtag());
        
        // The etag should be new and the modified time should be greater than the previous modified time
        Long endModifiedOn = clone2.getModifiedOn().getTime();
        assertTrue("Modified On should have change and be greater than the previous timestamp", endModifiedOn > startModifiedOn);
        
        // Make sure the attachments for this wiki are updated to have 2 files
        assertEquals(clone.getAttachmentFileHandleIds(), clone2.getAttachmentFileHandleIds());
        
        List<Long> reservationIds = wikiPageDao.getFileHandleReservationForWiki(key);
        // The archive should have only added one more entry
        assertTrue(reservationIds.size() == 2);

        clone2.setMarkdownFileHandleId(markdownTwo.getId());
        // Update with same fileNameMap
        V2WikiPage clone3 = wikiPageDao.updateWikiPage(clone2, fileNameMap, ownerId, ownerType, new ArrayList<String>());                
        List<Long> reservationIds2 = wikiPageDao.getFileHandleReservationForWiki(key);                
        // the toInsert list of attachments should be 0 and the archive should still be size 2
        assertTrue(reservationIds2.size() == 2);
	}
	
	@Test
	public void testParentCycle() throws NotFoundException {
		// When updating the parent wiki id, the check for valid parent ids
		// should ensure that the update will not create a cycle. 
		String ownerId = "syn182";
		ObjectType ownerType = ObjectType.ENTITY;
		
		V2WikiPage grandparent = new V2WikiPage();
		grandparent.setTitle("Title");
		grandparent.setCreatedBy(creatorUserGroupId);
		grandparent.setModifiedBy(creatorUserGroupId);
		grandparent.setMarkdownFileHandleId(markdownOne.getId());
		grandparent.setAttachmentFileHandleIds(new LinkedList<String>());
		grandparent = wikiPageDao.create(grandparent, new HashMap<String, FileHandle>(), ownerId, ownerType, new ArrayList<String>());
		WikiPageKey key = new WikiPageKey(ownerId, ownerType, grandparent.getId());
		toDelete.add(key);
		
		V2WikiPage parent = new V2WikiPage();
		parent.setParentWikiId(grandparent.getId());
		parent.setTitle("Title");
		parent.setCreatedBy(creatorUserGroupId);
		parent.setModifiedBy(creatorUserGroupId);
		parent.setMarkdownFileHandleId(markdownOne.getId());
		parent.setAttachmentFileHandleIds(new LinkedList<String>());
		parent = wikiPageDao.create(parent, new HashMap<String, FileHandle>(), ownerId, ownerType, new ArrayList<String>());
		WikiPageKey key2 = new WikiPageKey(ownerId, ownerType, parent.getId());
		toDelete.add(key2);
		
		V2WikiPage child = new V2WikiPage();
		child.setParentWikiId(parent.getId());
		child.setTitle("Title");
		child.setCreatedBy(creatorUserGroupId);
		child.setModifiedBy(creatorUserGroupId);
		child.setMarkdownFileHandleId(markdownOne.getId());
		child.setAttachmentFileHandleIds(new LinkedList<String>());
		child = wikiPageDao.create(child, new HashMap<String, FileHandle>(), ownerId, ownerType, new ArrayList<String>());
		WikiPageKey key3 = new WikiPageKey(ownerId, ownerType, child.getId());
		toDelete.add(key3);
		
		grandparent.setParentWikiId(child.getId());
		// This creates a cycle
		try {
			grandparent = wikiPageDao.updateWikiPage(grandparent, new HashMap<String, FileHandle>(), ownerId, ownerType, new ArrayList<String>());
			fail("Should have thrown an exception because this update makes a cycle.");
		} catch(IllegalArgumentException e) {
			// expected
		}

		// Create a wiki with its parent id equal to its own id
		// Should detect this short cycle.
		V2WikiPage thirdChild = new V2WikiPage();
		thirdChild.setId("100");
		thirdChild.setParentWikiId("100");
		thirdChild.setTitle("Title");
		thirdChild.setCreatedBy(creatorUserGroupId);
		thirdChild.setModifiedBy(creatorUserGroupId);
		thirdChild.setMarkdownFileHandleId(markdownOne.getId());
		thirdChild.setAttachmentFileHandleIds(new LinkedList<String>());
		try {
			thirdChild = wikiPageDao.create(thirdChild, new HashMap<String, FileHandle>(), ownerId, ownerType, new ArrayList<String>());
			fail("Should have failed because creating this wiki creates a cycle.");
		} catch(Exception e) {
			// expected
		}
	}
	
	@Test
	public void testGetWikiHistoryAndRestore() throws NotFoundException, InterruptedException {
		V2WikiPage page = new V2WikiPage();
		String ownerId = "syn1082";
		ObjectType ownerType = ObjectType.EVALUATION;
		page.setTitle("Title");
		page.setCreatedBy(creatorUserGroupId);
		page.setModifiedBy(creatorUserGroupId);
		page.setMarkdownFileHandleId(markdownOne.getId());
		
		// Add an attachment
		page.setAttachmentFileHandleIds(new LinkedList<String>());
		page.getAttachmentFileHandleIds().add(attachOne.getId());
		Map<String, FileHandle> fileNameMap = new HashMap<String, FileHandle>();
		fileNameMap.put(attachOne.getFileName(), attachOne);
		List<String> newIds = new ArrayList<String>();
		newIds.add(attachOne.getId());
		
		// Create it
		V2WikiPage clone = wikiPageDao.create(page, fileNameMap, ownerId, ownerType, newIds);
		assertNotNull(clone);

		// Sleep to ensure the next date is higher.
		Thread.sleep(1000);
		 
		WikiPageKey key = new WikiPageKey(ownerId, ownerType, clone.getId());
		toDelete.add(key);
		
		// Add another attachment to the list and update markdown filehandle id to new markdown
		clone.getAttachmentFileHandleIds().add(attachTwo.getId());
		fileNameMap.put(attachTwo.getFileName(), attachTwo);
		clone.setMarkdownFileHandleId(markdownTwo.getId());
		
		List<String> newIds2 = new ArrayList<String>();
		newIds2.add(attachTwo.getId());
		
		// Update
		V2WikiPage clone2 = wikiPageDao.updateWikiPage(clone, fileNameMap, ownerId, ownerType, newIds2);		assertNotNull(clone2);
		assertTrue(clone2.getMarkdownFileHandleId().equals(markdownTwo.getId()));

		// At this point, the markdown database has two versions for this wiki page
		// Make sure history is accurate
		List<V2WikiHistorySnapshot> history = wikiPageDao.getWikiHistory(key, new Long(10), new Long(0));
		assertTrue(history.size() == 2);
		
		// history.get(0) is the most recent snapshot (the recently updated page)
		// history.get(1) older snapshot
		V2WikiHistorySnapshot oldWikiVersion = history.get(1);
		V2WikiHistorySnapshot currentWikiVersion = history.get(0);
		assertTrue(currentWikiVersion.getModifiedOn().getTime() > oldWikiVersion.getModifiedOn().getTime());
		assertTrue(Long.parseLong(currentWikiVersion.getVersion()) > Long.parseLong(oldWikiVersion.getVersion()));
		
		// Test that correct versions are being accessed
		String currentMarkdownFileHandleId = wikiPageDao.getMarkdownHandleIdFromHistory(key, Long.parseLong(currentWikiVersion.getVersion()));
		List<String> currentAttachmentIds = wikiPageDao.getWikiFileHandleIdsFromHistory(key, Long.parseLong(currentWikiVersion.getVersion()));
		assertTrue(currentMarkdownFileHandleId.equals(markdownTwo.getId()));
		assertTrue(currentAttachmentIds.size() == 2);
		assertTrue(currentAttachmentIds.contains(attachOne.getId()) && currentAttachmentIds.contains(attachTwo.getId()));
		
		// To restore wiki to a the oldWikiVersion, download old version of attachments and markdown, set, and update again
		String oldMarkdownFileHandleId = wikiPageDao.getMarkdownHandleIdFromHistory(key, Long.parseLong(oldWikiVersion.getVersion()));
		List<String> oldAttachmentIds = wikiPageDao.getWikiFileHandleIdsFromHistory(key, Long.parseLong(oldWikiVersion.getVersion()));
		// Test getMarkdownHandleIdFromHistory and getWikiFileHandleIdsFromHistory
		assertTrue(oldMarkdownFileHandleId.equals(markdownOne.getId()));
		assertTrue(oldAttachmentIds.size() == 1);
		assertTrue(oldAttachmentIds.get(0).equals(attachOne.getId()));
		
		clone2.setMarkdownFileHandleId(oldMarkdownFileHandleId);
		clone2.setAttachmentFileHandleIds(oldAttachmentIds);
		fileNameMap.remove(attachTwo.getFileName());
		
		// Update
		V2WikiPage restored = wikiPageDao.updateWikiPage(clone2, fileNameMap, ownerId, ownerType, new ArrayList<String>());		assertNotNull(restored);
		
		assertTrue(restored.getMarkdownFileHandleId().equals(markdownOne.getId()));
		assertTrue(restored.getAttachmentFileHandleIds().size() == 1);
		
		// At this point, the restored wiki is the third version of the markdown/attachments
		List<V2WikiHistorySnapshot> historyAfterRestoration = wikiPageDao.getWikiHistory(key, new Long(10), new Long(0));
		assertTrue(historyAfterRestoration.size() == 3);
		
	}
	
	@Test
	public void testGet() throws NotFoundException {
		// Create a new wiki page with a single attachment
		V2WikiPage page = new V2WikiPage();
		String ownerId = "syn182";
		ObjectType ownerType = ObjectType.ENTITY;
		page.setTitle("Title");
		page.setCreatedBy(creatorUserGroupId);
		page.setModifiedBy(creatorUserGroupId);
		page.setMarkdownFileHandleId(markdownOne.getId());
		
		// Add an attachment
		page.setAttachmentFileHandleIds(new LinkedList<String>());
		page.getAttachmentFileHandleIds().add(attachOne.getId());
		Map<String, FileHandle> fileNameMap = new HashMap<String, FileHandle>();
		fileNameMap.put(attachOne.getFileName(), attachOne);
		List<String> newIds = new ArrayList<String>();
		newIds.add(attachOne.getId());
		
		// Create it
		V2WikiPage clone = wikiPageDao.create(page, fileNameMap, ownerId, ownerType, newIds);
		assertNotNull(clone);
		assertNotNull(clone.getId());
		
		WikiPageKey key = new WikiPageKey(ownerId, ownerType, clone.getId());
		toDelete.add(key);

		// Get wiki with key
		V2WikiPage retrievedWiki = wikiPageDao.get(key);
		// Compare the wiki pages
		assertEquals(retrievedWiki.getAttachmentFileHandleIds(), clone.getAttachmentFileHandleIds());
		assertEquals(retrievedWiki.getMarkdownFileHandleId(), clone.getMarkdownFileHandleId());
		assertEquals(retrievedWiki.getParentWikiId(), clone.getParentWikiId());
		assertEquals(retrievedWiki.getEtag(), clone.getEtag());
		assertTrue(retrievedWiki.equals(clone));
	}
	
	/**
	 * Create hierarchy of wiki pages
	 * @throws NotFoundException 
	 */
	@Test
	public void testWikiHierarchyAndSettingRoots() throws NotFoundException{
		V2WikiPage root = new V2WikiPage();
		String ownerId = "syn2224";
		ObjectType ownerType = ObjectType.ENTITY;
		root.setTitle("Root");
		root.setCreatedBy(creatorUserGroupId);
		root.setModifiedBy(creatorUserGroupId);
		root.setMarkdownFileHandleId(markdownOne.getId());
		
		// Create it
		root = wikiPageDao.create(root, new HashMap<String, FileHandle>(), ownerId, ownerType, new ArrayList<String>());
		assertNotNull(root);
		String rootId = root.getId();
		assertNotNull(rootId);
		
		WikiPageKey rootKey = new WikiPageKey(ownerId, ownerType, rootId);
		toDelete.add(rootKey);
		
		// In setRoot, passed through first branch because root == parent
		// Root id should be set it itself
		long rootIdForParent = simpleJdbcTemplate.queryForLong(SQL_GET_ROOT_ID, root.getId());	
		assertEquals(String.valueOf(rootIdForParent), rootId);
		
		// Add add children in reverse alphabetical order.
		int childCount = 3;
		List<V2WikiPage> children = new LinkedList<V2WikiPage>();
		for(int i=childCount-1; i>-1; i--){
			V2WikiPage child = new V2WikiPage();
			child.setTitle("A"+i);
			child.setCreatedBy(creatorUserGroupId);
			child.setModifiedBy(creatorUserGroupId);
			// Set parent id before creating children
			// In setRoot, passes through else branch because parent id is not null
			child.setParentWikiId(root.getId());
			child.setMarkdownFileHandleId(markdownOne.getId());
			child = wikiPageDao.create(child, new HashMap<String, FileHandle>(), ownerId, ownerType, new ArrayList<String>());
			children.add(child);
		}
		
		// Test one child; root should be set to parent root's id
		long rootIdForChild = simpleJdbcTemplate.queryForLong(SQL_GET_ROOT_ID, children.get(0).getId());	
		assertEquals(String.valueOf(rootIdForChild), rootId);
		
		// Test getRootWiki for this hierarchy
		Long rootWikiId = wikiPageDao.getRootWiki(ownerId, ownerType);
		assertTrue(rootWikiId.equals(new Long(rootId)));
		
		// Test getHeaderTree
		// Should return tree (including the parent)
		List<V2WikiHeader> list = wikiPageDao.getHeaderTree(ownerId, ownerType);
		System.out.println(list);
		assertNotNull(list);
		assertEquals(childCount+1, list.size());
		
		// Check order; the parent should be first
		assertEquals("Root", list.get(0).getTitle());
		assertEquals("A0", list.get(1).getTitle());
		assertEquals("A"+(childCount-1), list.get(childCount).getTitle());
		
		// Check cascade delete
		wikiPageDao.delete(rootKey);
		for(V2WikiPage childWiki: children){
			try{
				wikiPageDao.get(new WikiPageKey(ownerId, ownerType, childWiki.getId()));
				fail("This child should have been deleted when the parent was deleted.");
			}catch(NotFoundException e){
				// expected
			}
		}
		
	}
	
	@Test
	public void testDelete() throws NotFoundException, InterruptedException{
		V2WikiPage page = new V2WikiPage();
		String ownerId = "syn2082";
		ObjectType ownerType = ObjectType.EVALUATION;
		page.setTitle("Title");
		page.setCreatedBy(creatorUserGroupId);
		page.setModifiedBy(creatorUserGroupId);
		page.setMarkdownFileHandleId(markdownOne.getId());
		
		// Add an attachment
		page.setAttachmentFileHandleIds(new LinkedList<String>());
		page.getAttachmentFileHandleIds().add(attachOne.getId());
		Map<String, FileHandle> fileNameMap = new HashMap<String, FileHandle>();
		fileNameMap.put(attachOne.getFileName(), attachOne);
		List<String> newIds = new ArrayList<String>();
		newIds.add(attachOne.getId());
		
		// Create it
		V2WikiPage clone = wikiPageDao.create(page, fileNameMap, ownerId, ownerType, newIds);
		assertNotNull(clone);
		WikiPageKey key = new WikiPageKey(ownerId, ownerType, clone.getId());
		toDelete.add(key);
		
		List<V2WikiHistorySnapshot> historyBeforeUpdate = wikiPageDao.getWikiHistory(key, new Long(10), new Long(0));
		assertTrue(historyBeforeUpdate.size() == 1);

		Thread.sleep(1000);
		
		// Add another attachment to the list and update markdown filehandle id to another markdown
		clone.getAttachmentFileHandleIds().add(attachTwo.getId());
		clone.setMarkdownFileHandleId(markdownTwo.getId());
		fileNameMap.put(attachTwo.getFileName(), attachTwo);
		
		List<String> newIds2 = new ArrayList<String>();
		newIds2.add(attachTwo.getId());
		
		// Update
		V2WikiPage clone2 = wikiPageDao.updateWikiPage(clone, fileNameMap, ownerId, ownerType, newIds2);		assertNotNull(clone2);
		assertNotNull(clone2.getEtag());
		
		List<V2WikiHistorySnapshot> historyAfterUpdate = wikiPageDao.getWikiHistory(key, new Long(10), new Long(0));
		assertTrue(historyAfterUpdate.size() == 2);
		
		// Check cascade delete
		wikiPageDao.delete(key);
		try {
			wikiPageDao.getWikiHistory(key, new Long(10), new Long(0));
			fail("Should not be able to access history of a deleted wiki.");
		} catch(NotFoundException e) {
			// expected
		}
	}
	
	/**
	 * Gets a list of a wiki page's file handle ids
	 * @throws NotFoundException 
	 */
	@Test
	public void testGetWikiFileHandleIds() throws NotFoundException{
		V2WikiPage root = new V2WikiPage();
		String ownerId = "syn3082";
		ObjectType ownerType = ObjectType.ENTITY;
		root.setTitle("Root");
		root.setCreatedBy(creatorUserGroupId);
		root.setModifiedBy(creatorUserGroupId);
		root.setMarkdownFileHandleId(markdownOne.getId());
		root.setAttachmentFileHandleIds(new LinkedList<String>());
		// Add file handles to the root.
		root.getAttachmentFileHandleIds().add(attachOne.getId());
		root.getAttachmentFileHandleIds().add(attachTwo.getId());
		Map<String, FileHandle> fileNameMap = new HashMap<String, FileHandle>();
		fileNameMap.put(attachOne.getFileName(), attachOne);
		fileNameMap.put(attachTwo.getFileName(), attachTwo);
		List<String> newIds = new ArrayList<String>();
		newIds.add(attachOne.getId());
		newIds.add(attachTwo.getId());
		
		// Create it
		root = wikiPageDao.create(root, fileNameMap, ownerId, ownerType, newIds);
		assertNotNull(root);
		WikiPageKey rootKey = new WikiPageKey(ownerId, ownerType, root.getId());
		toDelete.add(rootKey);
		
		// Test the parent for the FileHandleIds
		List<String> handleList = wikiPageDao.getWikiFileHandleIds(rootKey);
		assertNotNull(handleList);
		assertEquals(2, handleList.size());
		assertEquals(attachOne.getId(), handleList.get(0));
		assertEquals(attachTwo.getId(), handleList.get(1));
		
		// Add a child.
		V2WikiPage child = new V2WikiPage();
		child.setTitle("Child");
		child.setCreatedBy(creatorUserGroupId);
		child.setModifiedBy(creatorUserGroupId);
		child.setMarkdownFileHandleId(markdownOne.getId());
		child.setParentWikiId(root.getId());
		// Add one file handle to the child.
		child.setAttachmentFileHandleIds(new LinkedList<String>());
		child.getAttachmentFileHandleIds().add(attachOne.getId());
		Map<String, FileHandle> childFileNameMap = new HashMap<String, FileHandle>();
		childFileNameMap.put(attachOne.getFileName(), attachOne);
		List<String> childNewIds = new ArrayList<String>();
		childNewIds.add(attachOne.getId());
		
		// Create it
		child = wikiPageDao.create(child, childFileNameMap, ownerId, ownerType, childNewIds);
		WikiPageKey childKey = new WikiPageKey(ownerId, ownerType, child.getId());
		
		// Test the child for the FileHandleId
		List<String> childHandleList = wikiPageDao.getWikiFileHandleIds(childKey);
		assertNotNull(childHandleList);
		assertEquals(1, childHandleList.size());
		assertTrue(childHandleList.contains(attachOne.getId()));
	}
	
	/**
	 * Looks up file handles for wiki pages by name
	 * @throws Exception
	 */
	@Test
	public void testgetWikiAttachmentFileHandleForFileName() throws Exception{
		V2WikiPage root = new V2WikiPage();
		String ownerId = "syn4082";
		ObjectType ownerType = ObjectType.ENTITY;
		root.setTitle("Root");
		root.setCreatedBy(creatorUserGroupId);
		root.setModifiedBy(creatorUserGroupId);
		root.setMarkdownFileHandleId(markdownOne.getId());
		root.setAttachmentFileHandleIds(new LinkedList<String>());
		root.getAttachmentFileHandleIds().add(attachOne.getId());
		root.getAttachmentFileHandleIds().add(attachTwo.getId());
		Map<String, FileHandle> fileNameMap = new HashMap<String, FileHandle>();
		fileNameMap.put(attachOne.getFileName(), attachOne);
		fileNameMap.put(attachTwo.getFileName(), attachTwo);
		List<String> newIds = new ArrayList<String>();
		newIds.add(attachOne.getId());
		newIds.add(attachTwo.getId());
		root = wikiPageDao.create(root, fileNameMap, ownerId, ownerType, newIds);
		assertNotNull(root);
		WikiPageKey key = new WikiPageKey(ownerId, ownerType, root.getId());
		toDelete.add(key);
		
		// Now lookup each file using its name.
		String id = wikiPageDao.getWikiAttachmentFileHandleForFileName(key, attachOne.getFileName());
		assertEquals(attachOne.getId(), id);
		// The second
		id = wikiPageDao.getWikiAttachmentFileHandleForFileName(key, attachTwo.getFileName());
		assertEquals(attachTwo.getId(), id);
		
		// Test the not found case
		try{
			wikiPageDao.getWikiAttachmentFileHandleForFileName(key, attachTwo.getFileName()+"1");
			fail("The file name does not exist and should have failed");
		}catch(NotFoundException e){
			// expected
		}
	}
	
}
