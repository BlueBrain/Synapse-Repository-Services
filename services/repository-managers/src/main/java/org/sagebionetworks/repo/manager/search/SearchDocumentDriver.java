package org.sagebionetworks.repo.manager.search;

import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * @author deflaux
 *
 */
public interface SearchDocumentDriver {
	
	/**
	 * Does the given document exist.
	 * @param nodeId
	 * @param etag
	 * @return
	 */
	public boolean doesDocumentExist(String nodeId, String etag);
	
	/**
	 * Create a search document for a given NodeId.
	 * @param nodeId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public Document formulateSearchDocument(String nodeId) throws DatastoreException, NotFoundException;
	/**
	 * Create a search document and return it.
	 * 
	 * @param node
	 * @param rev
	 * @param acl
	 * @return the search document for the node
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public Document formulateSearchDocument(Node node, NamedAnnotations annos,
			AccessControlList acl, EntityPath entityPath, String wikiPagesText) throws DatastoreException, NotFoundException;
	
	/**
	 * 
	 * @param nodeId
	 * @return
	 * @throws NotFoundException
	 */
	public EntityPath getEntityPath(String nodeId) throws NotFoundException;
	
	
	public String getAllWikiPageText(String nodeId) throws DatastoreException;
	

}