package org.sagebionetworks.repo.model;

import java.util.List;

import org.sagebionetworks.repo.model.message.MessageBundle;
import org.sagebionetworks.repo.model.message.MessageSortBy;
import org.sagebionetworks.repo.model.message.MessageStatusType;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.web.NotFoundException;

public interface MessageDAO {
	
	/**
	 * Retrieves a message by ID
	 */
	public MessageToUser getMessage(String messageId) throws NotFoundException;

	/**
	 * Saves the message information so that it can be processed by a worker
	 */
	public MessageToUser createMessage(MessageToUser dto);
	
	/**
	 * Changes the etag of a message
	 */
	public void touch(String messageId);
	
	/**
	 * Retrieves all messages (subject to limit and offset) within a given thread, visible to the user
	 * @param sortBy What value to sort the results by
	 */
	public List<MessageToUser> getConversation(String rootMessageId, String userId, MessageSortBy sortBy, boolean descending, long limit, long offset);
	
	/**
	 * Returns the number of messages within the thread
	 */
	public long getConversationSize(String rootMessageId, String userId);
	
	/**
	 * Retrieves all messages (subject to limit and offset) received by the user
	 * @param sortBy What value to sort the results by
	 */
	public List<MessageBundle> getReceivedMessages(String userId, List<MessageStatusType> included, MessageSortBy sortBy, boolean descending, long limit, long offset);
	
	/**
	 * Returns the number of messages received by the user
	 */
	public long getNumReceivedMessages(String userId, List<MessageStatusType> included);
	
	/**
	 * Retrieves all messages (subject to limit and offset) sent by the user
	 * @param sortBy What value to sort the results by
	 */
	public List<MessageToUser> getSentMessages(String userId, MessageSortBy sortBy, boolean descending, long limit, long offset);
	
	/**
	 * Returns the number of messages sent by the user
	 */
	public long getNumSentMessages(String userId);
	
	/**
	 * See {@link #createMessageStatus(String, String, MessageStatusType)}
	 * The status of the message defaults to UNREAD
	 */
	public void createMessageStatus(String messageId, String userId);
	
	/**
	 * Marks a user as a recipient of a message
	 */
	public void createMessageStatus(String messageId, String userId, MessageStatusType status);
	
	/**
	 * Marks a message within the user's inbox with the given status
	 */
	public void updateMessageStatus(String messageId, String userId, MessageStatusType status);
}
