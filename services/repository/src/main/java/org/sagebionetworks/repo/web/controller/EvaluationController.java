package org.sagebionetworks.repo.web.controller;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.evaluation.model.Participant;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionBundle;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.evaluation.model.UserEvaluationPermissions;
import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.BooleanResult;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.query.QueryTableResults;
import org.sagebionetworks.repo.queryparser.ParseException;
import org.sagebionetworks.repo.util.ControllerUtil;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.sagebionetworks.repo.web.service.ServiceProvider;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Provides access to all services related to Synapse Evaluations. Evaluations are designed to
 * support open, collaborative data analysis challenges in Synapse.
 * 
 * @author bkng
 */
@ControllerInfo(displayName="Evaluation Services", path="repo/v1")
@Controller
public class EvaluationController extends BaseController {

	@Autowired
	ServiceProvider serviceProvider;

	/**
	 * Create a new Evaluation.
	 * 
	 * @param userId
	 * @param header
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 * @throws JSONObjectAdapterException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.EVALUATION, method = RequestMethod.POST)
	public @ResponseBody
	Evaluation createEvaluation(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestHeader HttpHeaders header,
			HttpServletRequest request
			) throws DatastoreException, InvalidModelException, NotFoundException, JSONObjectAdapterException
	{
		String requestBody = ControllerUtil.getRequestBodyAsString(request);
		Evaluation eval = new Evaluation(new JSONObjectAdapterImpl(requestBody));
		return serviceProvider.getEvaluationService().createEvaluation(userId, eval);
	}
	
	/**
	 * Get an Evaluation.
	 * 
	 * @param evalId
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EVALUATION_WITH_ID, method = RequestMethod.GET)
	public @ResponseBody
	Evaluation getEvaluation(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String evalId,
			HttpServletRequest request
			) throws DatastoreException, UnauthorizedException, NotFoundException 
	{
		return serviceProvider.getEvaluationService().getEvaluation(userId, evalId);
	}
	
	/**
	 * Get a collection of Evaluations, within a given range.
	 * 
	 * @param offset
	 * @param limit
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EVALUATION, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<Evaluation> getEvaluationsPaginated(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) long offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) long limit,
			HttpServletRequest request
			) throws DatastoreException, NotFoundException
	{
		return serviceProvider.getEvaluationService().getEvaluationsInRange(userId, limit, offset, request);
	}
	
	/**
	 * Get a collection of Evaluations in which the user may participate, within a given range.
	 * 
	 * @param offset
	 * @param limit
	 * @param userId
	 * @param statusString
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EVALUATION_AVAILABLE, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<Evaluation> getAvailableEvaluationsPaginated(
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) long offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) long limit,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestParam(value = UrlHelpers.STATUS, defaultValue = "") String statusString,
			HttpServletRequest request
			) throws DatastoreException, NotFoundException
	{
		EvaluationStatus status = null;
		if (statusString.length() > 0) {
			status = EvaluationStatus.valueOf(statusString.toUpperCase().trim());
		}
		return serviceProvider.getEvaluationService().getAvailableEvaluationsInRange(userId, status, limit, offset, request);
	}	
	
	/**
	 * Get the total number of Evaluations in Synapse.
	 * 
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EVALUATION_COUNT, method = RequestMethod.GET)
	public @ResponseBody
	long getEvaluationCount(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			HttpServletRequest request) throws DatastoreException, NotFoundException
	{
		return serviceProvider.getEvaluationService().getEvaluationCount(userId);
	}
	
	/**
	 * Find an Evaluation by name.
	 * 
	 * @param name
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 * @throws UnsupportedEncodingException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EVALUATION_WITH_NAME, method = RequestMethod.GET)
	public @ResponseBody
	Evaluation findEvaluation(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String name,
			HttpServletRequest request
			) throws DatastoreException, UnauthorizedException, NotFoundException, UnsupportedEncodingException 
	{
		String decodedName = URLDecoder.decode(name, "UTF-8");
		return serviceProvider.getEvaluationService().findEvaluation(userId, decodedName);
	}
	
	/**
	 * Update an Evaluation.
	 * 
	 * @param evalId
	 * @param userId
	 * @param header
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws InvalidModelException
	 * @throws ConflictingUpdateException
	 * @throws NotFoundException
	 * @throws JSONObjectAdapterException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EVALUATION_WITH_ID, method = RequestMethod.PUT)
	public @ResponseBody
	Evaluation updateEvaluation(
			@PathVariable String evalId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestHeader HttpHeaders header,
			HttpServletRequest request) throws DatastoreException, UnauthorizedException, InvalidModelException, ConflictingUpdateException, NotFoundException, JSONObjectAdapterException
	{
		String requestBody = ControllerUtil.getRequestBodyAsString(request);
		Evaluation eval = new Evaluation(new JSONObjectAdapterImpl(requestBody));
		if (!evalId.equals(eval.getId()))
			throw new IllegalArgumentException("Evaluation ID does not match requested ID: " + evalId);
		return serviceProvider.getEvaluationService().updateEvaluation(userId, eval);
	}
	
	/**
	 * Delete an Evaluation.
	 * 
	 * @param evalId
	 * @param userId
	 * @param header
	 * @param request
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.EVALUATION_WITH_ID, method = RequestMethod.DELETE)
	public @ResponseBody
	void deleteEvaluation(
			@PathVariable String evalId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestHeader HttpHeaders header,
			HttpServletRequest request) throws DatastoreException, UnauthorizedException, NotFoundException 
	{
		serviceProvider.getEvaluationService().deleteEvaluation(userId, evalId);
	}
	
	/**
	 * Join as a Participant in a specified Evaluation.
	 * 
	 * @param evalId
	 * @param userId
	 * @param header
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.PARTICIPANT, method = RequestMethod.POST)
	public @ResponseBody
	Participant createParticipant(
			@PathVariable String evalId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestHeader HttpHeaders header,
			HttpServletRequest request
			) throws DatastoreException, InvalidModelException, NotFoundException
	{
		return serviceProvider.getEvaluationService().addParticipant(userId, evalId);
	}
	
	/**
	 * Get a Participant.
	 * 
	 * @param evalId
	 * @param partId
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.PARTICIPANT_WITH_ID, method = RequestMethod.GET)
	public @ResponseBody
	Participant getParticipant(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String evalId,
			@PathVariable String partId,
			HttpServletRequest request
			) throws DatastoreException, UnauthorizedException, NotFoundException 
	{
		return serviceProvider.getEvaluationService().getParticipant(userId, partId, evalId);
	}
	
	/**
	 * Delete a Participant. Requires admin rights on the Evaluation.
	 * 
	 * @param evalId
	 * @param partId
	 * @param userId
	 * @param header
	 * @param request
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.PARTICIPANT_WITH_ID, method = RequestMethod.DELETE)
	public @ResponseBody
	void deleteParticipantAsAdmin(
			@PathVariable String evalId,
			@PathVariable String partId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestHeader HttpHeaders header,
			HttpServletRequest request
			) throws DatastoreException, InvalidModelException, NotFoundException
	{
		serviceProvider.getEvaluationService().removeParticipant(userId, evalId, partId);
	}
	
	/**
	 * Get all Participants for a specified Evaluation.
	 * 
	 * @param offset
	 * @param limit
	 * @param evalId
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.PARTICIPANT, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<Participant> getAllParticipants(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) long offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) long limit,
			@PathVariable String evalId,
			HttpServletRequest request
			) throws DatastoreException, UnauthorizedException, NotFoundException 
	{
		return serviceProvider.getEvaluationService().getAllParticipants(userId, evalId, limit, offset, request);
	}
	
	/**
	 * Get the number of Participants in a specified Evaluation.
	 * 
	 * @param evalId
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.PARTICIPANT_COUNT, method = RequestMethod.GET)
	public @ResponseBody
	long getParticipantCount(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String evalId,
			HttpServletRequest request) 
			throws DatastoreException, NotFoundException
	{
		return serviceProvider.getEvaluationService().getParticipantCount(userId, evalId);
	}
	
	/**
	 * Create a Submission.
	 * 
	 * @param userId
	 * @param entityEtag - the current eTag of the Entity being submitted
	 * @param header
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 * @throws JSONObjectAdapterException
	 * @throws UnauthorizedException
	 * @throws ACLInheritanceException
	 * @throws ParseException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.SUBMISSION, method = RequestMethod.POST)
	public @ResponseBody
	Submission createSubmission(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestParam(value = AuthorizationConstants.ETAG_PARAM, required = false) String entityEtag,
			@RequestHeader HttpHeaders header,
			HttpServletRequest request
			) throws DatastoreException, InvalidModelException, NotFoundException, JSONObjectAdapterException, UnauthorizedException, ACLInheritanceException, ParseException
	{
		String requestBody = ControllerUtil.getRequestBodyAsString(request);
		Submission sub = new Submission(new JSONObjectAdapterImpl(requestBody));
		return serviceProvider.getEvaluationService().createSubmission(userId, sub, entityEtag, request);
	}
	
	/**
	 * Get a Submission.
	 * 
	 * @param subId
	 * @param userId
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SUBMISSION_WITH_ID, method = RequestMethod.GET)
	public @ResponseBody
	Submission getSubmission(
			@PathVariable String subId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			HttpServletRequest request
			) throws DatastoreException, UnauthorizedException, NotFoundException 
	{
		return serviceProvider.getEvaluationService().getSubmission(userId, subId);
	}
	
	/**
	 * Get the SubmissionStatus object associated with a specified Submission. This object includes
	 * scoring information.
	 * 
	 * @param subId
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SUBMISSION_STATUS, method = RequestMethod.GET)
	public @ResponseBody
	SubmissionStatus getSubmissionStatus(
			@PathVariable String subId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			HttpServletRequest request
			) throws DatastoreException, UnauthorizedException, NotFoundException 
	{
		return serviceProvider.getEvaluationService().getSubmissionStatus(userId, subId);
	}
	
	/**
	 * Update a SubmissionStatus object.
	 *  
	 * @param subId
	 * @param userId
	 * @param header
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws InvalidModelException
	 * @throws ConflictingUpdateException
	 * @throws NotFoundException
	 * @throws JSONObjectAdapterException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SUBMISSION_STATUS, method = RequestMethod.PUT)
	public @ResponseBody
	SubmissionStatus updateSubmissionStatus(
			@PathVariable String subId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestHeader HttpHeaders header,
			HttpServletRequest request) 
			throws DatastoreException, UnauthorizedException, InvalidModelException, 
			ConflictingUpdateException, NotFoundException, JSONObjectAdapterException
	{
		String requestBody = ControllerUtil.getRequestBodyAsString(request);
		SubmissionStatus status = new SubmissionStatus(new JSONObjectAdapterImpl(requestBody));
		if (!subId.equals(status.getId()))
			throw new IllegalArgumentException("Submission ID does not match requested ID: " + subId);
		return serviceProvider.getEvaluationService().updateSubmissionStatus(userId, status);
	}
	
	/**	
	 * Delete a Submission. Requires admin rights on the Evaluation.
	 * 
	 * Use of this service is discouraged, since Submissions should be immutable.
	 * 
	 * @param subId
	 * @param userId
	 * @param header
	 * @param request
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@Deprecated
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.SUBMISSION_WITH_ID, method = RequestMethod.DELETE)
	public @ResponseBody
	void deleteSubmission(
			@PathVariable String subId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestHeader HttpHeaders header,
			HttpServletRequest request) throws DatastoreException, UnauthorizedException, NotFoundException 
	{
		serviceProvider.getEvaluationService().deleteSubmission(userId, subId);
	}
	
	/**
	 * Get a collection of Submissions to a given Evaluation. Requires admin rights on the
	 * Evaluation.
	 * 
	 * @param evalId
	 * @param offset
	 * @param limit
	 * @param userId
	 * @param statusString
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SUBMISSION_WITH_EVAL_ID_ADMIN, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<Submission> getAllSubmissions(
			@PathVariable String evalId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) long offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) long limit,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestParam(value = UrlHelpers.STATUS, defaultValue = "") String statusString,
			HttpServletRequest request
			) throws DatastoreException, UnauthorizedException, NotFoundException 
	{
		SubmissionStatusEnum status = null;
		if (statusString.length() > 0) {
			status = SubmissionStatusEnum.valueOf(statusString.toUpperCase().trim());
		}		
		return serviceProvider.getEvaluationService().getAllSubmissions(userId, evalId, status, limit, offset, request);
	}
	
	/**
	 * Get a collection of SubmissionStatuses to a given Evaluation. Requires admin rights on the
	 * Evaluation.
	 * 
	 * @param evalId
	 * @param offset
	 * @param limit
	 * @param statusString
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SUBMISSION_STATUS_WITH_EVAL_ID, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<SubmissionStatus> getAllSubmissionStatuses(
			@PathVariable String evalId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) long offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) long limit,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestParam(value = UrlHelpers.STATUS, defaultValue = "") String statusString,
			HttpServletRequest request
			) throws DatastoreException, UnauthorizedException, NotFoundException 
	{
		SubmissionStatusEnum status = null;
		if (statusString.length() > 0) {
			status = SubmissionStatusEnum.valueOf(statusString.toUpperCase().trim());
		}
		return serviceProvider.getEvaluationService().getAllSubmissionStatuses(userId, evalId, status, limit, offset, request);
	}
	
	/**
	 * Get a collection of bundled Submissions and SubmissionStatuses to a given Evaluation. 
	 * Requires admin rights on the Evaluation.
	 * 
	 * @param evalId
	 * @param offset
	 * @param limit
	 * @param userId
	 * @param statusString
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SUBMISSION_WITH_EVAL_ID_ADMIN_BUNDLE, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<SubmissionBundle> getAllSubmissionBundles(
			@PathVariable String evalId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) long offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) long limit,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestParam(value = UrlHelpers.STATUS, defaultValue = "") String statusString,
			HttpServletRequest request
			) throws DatastoreException, UnauthorizedException, NotFoundException 
	{
		SubmissionStatusEnum status = null;
		if (statusString.length() > 0) {
			status = SubmissionStatusEnum.valueOf(statusString.toUpperCase().trim());
		}		
		return serviceProvider.getEvaluationService().getAllSubmissionBundles(userId, evalId, status, limit, offset, request);
	}
	
	/**
	 * Get the requesting user's Submissions to a specified Evaluation.
	 * 
	 * @param evalId
	 * @param offset
	 * @param limit
	 * @param userId
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SUBMISSION_WITH_EVAL_ID, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<Submission> getMySubmissions(
			@PathVariable String evalId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) long offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) long limit,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			HttpServletRequest request
			) throws DatastoreException, UnauthorizedException, NotFoundException 
	{
		return serviceProvider.getEvaluationService().getAllSubmissionsByEvaluationAndUser(evalId, userId, limit, offset, request);
	}
	
	/**
	 * Get the requesting user's bundled Submissions and SubmissionStatuses to a specified
	 * Evaluation.
	 * 
	 * @param evalId
	 * @param offset
	 * @param limit
	 * @param userId
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SUBMISSION_WITH_EVAL_ID_BUNDLE, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<SubmissionBundle> getMySubmissionBundles(
			@PathVariable String evalId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) long offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) long limit,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			HttpServletRequest request
			) throws DatastoreException, UnauthorizedException, NotFoundException 
	{
		return serviceProvider.getEvaluationService().getAllSubmissionBundlesByEvaluationAndUser(evalId, userId, limit, offset, request);
	}
	
	/**
	 * Get a pre-signed URL to access a requested File contained within a
	 * specified Submission.
	 * 
	 * @param userInfo
	 * @param submissionId
	 * @param fileHandleId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException 
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SUBMISSION_FILE, method = RequestMethod.GET)
	public @ResponseBody
	void redirectURLForFileHandle(
			@PathVariable String subId,
			@PathVariable String fileHandleId,
			@RequestParam (required = false) Boolean redirect,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			HttpServletResponse response
			) throws DatastoreException, NotFoundException, IOException {
		URL url = serviceProvider.getEvaluationService().getRedirectURLForFileHandle(userId, subId, fileHandleId);
		RedirectUtils.handleRedirect(redirect, url, response);
	}
	
	/**
	 * Get the number of Submissions to a specified Evaluation.
	 * 
	 * @param evalId
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SUBMISSION_COUNT, method = RequestMethod.GET)
	public @ResponseBody
	long getSubmissionCount(
			@PathVariable String evalId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			HttpServletRequest request
			) throws DatastoreException, NotFoundException
	{
		return serviceProvider.getEvaluationService().getSubmissionCount(userId, evalId);
	}
	
	/**
	 * @param id 
	 * @param userId 
	 * @param accessType 
	 * @param request 
	 * @return the access types that the given user has to the given resource
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws UnauthorizedException 
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value={UrlHelpers.EVALUATION_WITH_ID+UrlHelpers.ACCESS}, method=RequestMethod.GET)
	public @ResponseBody BooleanResult hasAccess(
			@PathVariable String evalId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestParam(value = UrlHelpers.ACCESS_TYPE_PARAM, required = false) String accessType,
			HttpServletRequest request) throws DatastoreException, NotFoundException, UnauthorizedException {
		// pass it along.
		return new BooleanResult(serviceProvider.getEvaluationService().hasAccess(evalId, userId, request, accessType));
	}

	/**
<<<<<<< HEAD
	 * Updates the given ACL.
=======
	 * Create a new ACL.
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.EVALUATION_ACL, method = RequestMethod.POST)
	public @ResponseBody AccessControlList
	createAcl(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = true) String userId,
			@RequestBody AccessControlList acl,
			HttpServletRequest request)
			throws NotFoundException, DatastoreException, InvalidModelException,
			UnauthorizedException, ConflictingUpdateException {
		return serviceProvider.getEvaluationService().createAcl(userId, acl);
	}

	/**
	 * Update the given ACL.
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EVALUATION_ACL, method = RequestMethod.PUT)
	public @ResponseBody AccessControlList
	updateAcl(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = true) String userId,
			@RequestBody AccessControlList acl,
			HttpServletRequest request)
			throws NotFoundException, DatastoreException, InvalidModelException,
			UnauthorizedException, ConflictingUpdateException {
		return serviceProvider.getEvaluationService().updateAcl(userId, acl);
	}

	/**
	 * Delete the ACL of the specified evaluation.
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.EVALUATION_ID_ACL, method = RequestMethod.DELETE)
	public void deleteAcl(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = true) String userId,
			@PathVariable String evalId,
			HttpServletRequest request)
			throws NotFoundException, DatastoreException, InvalidModelException,
			UnauthorizedException, ConflictingUpdateException {
		serviceProvider.getEvaluationService().deleteAcl(userId, evalId);
	}

	/**
	 * Get the access control list (ACL) governing the given evaluation.
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EVALUATION_ID_ACL, method = RequestMethod.GET)
	public @ResponseBody AccessControlList
	getAcl(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = true) String userId,
			@PathVariable String evalId,
			HttpServletRequest request)
			throws NotFoundException, DatastoreException, ACLInheritanceException {
		return serviceProvider.getEvaluationService().getAcl(userId, evalId);
	}

	/**
	 * Gets the user permissions for the specified evaluation.
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EVALUATION_ID_PERMISSIONS, method = RequestMethod.GET)
	public @ResponseBody UserEvaluationPermissions
	getUserPermissionsForEvaluation(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = true) String userId,
			@PathVariable String evalId,
			HttpServletRequest request)
			throws NotFoundException, DatastoreException {
		return serviceProvider.getEvaluationService().getUserPermissionsForEvaluation(userId, evalId);
	}
	
	/**
	 * Execute a user-defined query over the Submissions of a specific Evaluation.
	 * 
	 * @throws JSONObjectAdapterException
	 * @throws ParseException 
	 * @throws  
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EVALUATION_QUERY, method = RequestMethod.GET)
	public @ResponseBody 
	QueryTableResults query(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = true) String userId,
			@RequestParam(value = ServiceConstants.QUERY_PARAM, required = true) String query,
			HttpServletRequest request)
			throws NotFoundException, DatastoreException, ParseException, 
			JSONObjectAdapterException {
		return serviceProvider.getEvaluationService().query(query, userId);
	}
}
