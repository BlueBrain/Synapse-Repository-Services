package org.sagebionetworks.repo.model;

import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Abstraction for the asynchronous DA.
 * 
 * This DAO will be used by an asynchronous process to update tables used as indices.
 * 
 * @author jmhill
 *
 */
public interface AsynchronousDAO {
	
	/**
	 * Called when an entity is created.
	 * @param id
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public void createEntity(String id) throws NotFoundException;
	
	/**
	 * Called when an entity is updated.
	 * @param id
	 * @throws NotFoundException 
	 */
	public void updateEntity(String id) throws NotFoundException;
	
	/**
	 * Called when an entity is deleted.
	 * @param id
	 */
	public void deleteEntity(String id);
}
