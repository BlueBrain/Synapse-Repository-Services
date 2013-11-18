/**
 * 
 */
package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GROUP_MEMBERS_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GROUP_MEMBERS_MEMBER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MEMBERSHIP_INVITATION_SUBMISSION_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MEMBERSHIP_INVITATION_SUBMISSION_EXPIRES_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MEMBERSHIP_INVITATION_SUBMISSION_INVITEE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MEMBERSHIP_INVITATION_SUBMISSION_PROPERTIES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MEMBERSHIP_INVITATION_SUBMISSION_TEAM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TEAM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.LIMIT_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.OFFSET_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_GROUP_MEMBERS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_MEMBERSHIP_INVITATION_SUBMISSION;

import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.MembershipInvitation;
import org.sagebionetworks.repo.model.MembershipInvtnSubmission;
import org.sagebionetworks.repo.model.MembershipInvtnSubmissionDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOMembershipInvtnSubmission;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


/**
 * @author brucehoff
 *
 */
public class DBOMembershipInvtnSubmissionDAOImpl implements MembershipInvtnSubmissionDAO {

	@Autowired
	private DBOBasicDao basicDao;	
	@Autowired
	private IdGenerator idGenerator;
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;
	@Autowired
	GroupMembersDAO groupMembersDAO;
	
	private static final String INVITEE_ID_COLUMN_LABEL = "INVITEE_ID";

	private static final String SELECT_OPEN_INVITATIONS_BY_USER_CORE = 
			" FROM "+TABLE_MEMBERSHIP_INVITATION_SUBMISSION+" mis "+
			" WHERE mis."+COL_MEMBERSHIP_INVITATION_SUBMISSION_INVITEE_ID+"=:"+COL_MEMBERSHIP_INVITATION_SUBMISSION_INVITEE_ID+
			" AND mis."+COL_MEMBERSHIP_INVITATION_SUBMISSION_TEAM_ID+" NOT IN (SELECT "+COL_GROUP_MEMBERS_GROUP_ID+" FROM "+
				TABLE_GROUP_MEMBERS+" WHERE "+COL_GROUP_MEMBERS_MEMBER_ID+"=mis."+COL_MEMBERSHIP_INVITATION_SUBMISSION_INVITEE_ID+" ) "+
			" AND ( mis."+COL_MEMBERSHIP_INVITATION_SUBMISSION_EXPIRES_ON+" IS NULL OR mis."+COL_MEMBERSHIP_INVITATION_SUBMISSION_EXPIRES_ON+">:"+COL_MEMBERSHIP_INVITATION_SUBMISSION_EXPIRES_ON+" ) ";
	
	private static final String SELECT_OPEN_INVITATIONS_BY_TEAM_AND_USER_CORE = 
			SELECT_OPEN_INVITATIONS_BY_USER_CORE+
			" AND mis."+COL_MEMBERSHIP_INVITATION_SUBMISSION_TEAM_ID+"=:"+COL_MEMBERSHIP_INVITATION_SUBMISSION_TEAM_ID;

	private static final String SELECT_OPEN_INVITATIONS_BY_USER_PAGINATED = 
			"SELECT mis.* "+SELECT_OPEN_INVITATIONS_BY_USER_CORE+
			" ORDER BY "+COL_MEMBERSHIP_INVITATION_SUBMISSION_CREATED_ON+" DESC "+
			" LIMIT :"+LIMIT_PARAM_NAME+" OFFSET :"+OFFSET_PARAM_NAME;
	
	private static final String SELECT_OPEN_INVITATIONS_BY_USER_COUNT = 
			"SELECT COUNT(*) "+ SELECT_OPEN_INVITATIONS_BY_USER_CORE;
	
	private static final String SELECT_OPEN_INVITATIONS_BY_TEAM_AND_USER_PAGINATED = 
			"SELECT mis.* "+
			SELECT_OPEN_INVITATIONS_BY_TEAM_AND_USER_CORE+
			" ORDER BY "+COL_MEMBERSHIP_INVITATION_SUBMISSION_CREATED_ON+" DESC "+
			" LIMIT :"+LIMIT_PARAM_NAME+" OFFSET :"+OFFSET_PARAM_NAME;
	
	private static final String SELECT_OPEN_INVITATIONS_BY_TEAM_AND_USER_COUNT = 
			"SELECT COUNT(*) "+SELECT_OPEN_INVITATIONS_BY_TEAM_AND_USER_CORE;
	
	private static final String DELETE_INVITATIONS_BY_TEAM_AND_INVITEE = 
			"DELETE FROM "+TABLE_MEMBERSHIP_INVITATION_SUBMISSION+" WHERE "+
			COL_MEMBERSHIP_INVITATION_SUBMISSION_TEAM_ID+"=:"+COL_MEMBERSHIP_INVITATION_SUBMISSION_TEAM_ID+
			" AND "+
			COL_MEMBERSHIP_INVITATION_SUBMISSION_INVITEE_ID+"=:"+COL_MEMBERSHIP_INVITATION_SUBMISSION_INVITEE_ID;

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.MemberRqstSubmissionDAO#create(org.sagebionetworks.repo.model.MemberRqstSubmission)
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public MembershipInvtnSubmission create(MembershipInvtnSubmission dto) throws DatastoreException,
	InvalidModelException {
		if (dto.getId()==null) dto.setId(idGenerator.generateNewId().toString());
		DBOMembershipInvtnSubmission dbo = new DBOMembershipInvtnSubmission();
		MembershipInvtnSubmissionUtils.copyDtoToDbo(dto, dbo);
		dbo = basicDao.createNew(dbo);
		MembershipInvtnSubmission result = MembershipInvtnSubmissionUtils.copyDboToDto(dbo);
		return result;
	}
	

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.MembershipInvtnSubmissionDAO#get(java.lang.String)
	 */
	@Override
	public MembershipInvtnSubmission get(String id) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_TEAM_ID.toLowerCase(), id);
		DBOMembershipInvtnSubmission dbo = basicDao.getObjectByPrimaryKey(DBOMembershipInvtnSubmission.class, param);
		MembershipInvtnSubmission dto = MembershipInvtnSubmissionUtils.copyDboToDto(dbo);
		return dto;
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.MembershipInvtnSubmissionDAO#delete(java.lang.String)
	 */
	@Override
	public void delete(String id) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_TEAM_ID.toLowerCase(), id);
		basicDao.deleteObjectByPrimaryKey(DBOMembershipInvtnSubmission.class, param);
	}
	
	private static final RowMapper<MembershipInvitation> membershipInvitationRowMapper = new RowMapper<MembershipInvitation>(){
		@Override
		public MembershipInvitation mapRow(ResultSet rs, int rowNum) throws SQLException {
			MembershipInvitation mi = new MembershipInvitation();
			mi.setUserId(rs.getString(INVITEE_ID_COLUMN_LABEL));
			mi.setTeamId(rs.getString(COL_MEMBERSHIP_INVITATION_SUBMISSION_TEAM_ID));
			mi.setExpiresOn(new Date(rs.getLong(COL_MEMBERSHIP_INVITATION_SUBMISSION_EXPIRES_ON)));
			Blob misProperties = rs.getBlob(COL_MEMBERSHIP_INVITATION_SUBMISSION_PROPERTIES);
			MembershipInvtnSubmission mis = MembershipInvtnSubmissionUtils.deserialize(misProperties.getBytes(1, (int) misProperties.length()));
			mi.setMessage(mis.getMessage());
			return mi;
		}
	};
	

	@Override
	public List<MembershipInvitation> getOpenByUserInRange(long userId, long now, long limit,
			long offset) throws DatastoreException {
		MapSqlParameterSource param = new MapSqlParameterSource();	
		param.addValue(COL_MEMBERSHIP_INVITATION_SUBMISSION_INVITEE_ID, userId);
		param.addValue(OFFSET_PARAM_NAME, offset);
		if (limit<=0) throw new IllegalArgumentException("'to' param must be greater than 'from' param.");
		param.addValue(LIMIT_PARAM_NAME, limit);	
		param.addValue(COL_MEMBERSHIP_INVITATION_SUBMISSION_EXPIRES_ON, now);	
		return simpleJdbcTemplate.query(SELECT_OPEN_INVITATIONS_BY_USER_PAGINATED, membershipInvitationRowMapper, param);
	}

	@Override
	public List<MembershipInvitation> getOpenByTeamAndUserInRange(
			long teamId, long userId, long now, long limit, long offset)
			throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();	
		param.addValue(COL_MEMBERSHIP_INVITATION_SUBMISSION_TEAM_ID, teamId);
		param.addValue(COL_MEMBERSHIP_INVITATION_SUBMISSION_INVITEE_ID, userId);
		param.addValue(OFFSET_PARAM_NAME, offset);
		if (limit<=0) throw new IllegalArgumentException("'to' param must be greater than 'from' param.");
		param.addValue(LIMIT_PARAM_NAME, limit);	
		param.addValue(COL_MEMBERSHIP_INVITATION_SUBMISSION_EXPIRES_ON, now);	
		return simpleJdbcTemplate.query(SELECT_OPEN_INVITATIONS_BY_TEAM_AND_USER_PAGINATED, membershipInvitationRowMapper, param);
	}

	@Override
	public long getOpenByUserCount(long userId, long now)
			throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();	
		param.addValue(COL_MEMBERSHIP_INVITATION_SUBMISSION_INVITEE_ID, userId);	
		param.addValue(COL_MEMBERSHIP_INVITATION_SUBMISSION_EXPIRES_ON, now);	
		return simpleJdbcTemplate.queryForLong(SELECT_OPEN_INVITATIONS_BY_USER_COUNT, param);
	}

	@Override
	public long getOpenByTeamAndUserCount(long teamId, long userId,
			long now) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();	
		param.addValue(COL_MEMBERSHIP_INVITATION_SUBMISSION_TEAM_ID, teamId);
		param.addValue(COL_MEMBERSHIP_INVITATION_SUBMISSION_INVITEE_ID, userId);
		param.addValue(COL_MEMBERSHIP_INVITATION_SUBMISSION_EXPIRES_ON, now);	
		return simpleJdbcTemplate.queryForLong(SELECT_OPEN_INVITATIONS_BY_TEAM_AND_USER_COUNT, param);
	}


	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteByTeamAndUser(long teamId, long inviteeId)
			throws DatastoreException {
		MapSqlParameterSource param = new MapSqlParameterSource();	
		param.addValue(COL_MEMBERSHIP_INVITATION_SUBMISSION_TEAM_ID, teamId);
		param.addValue(COL_MEMBERSHIP_INVITATION_SUBMISSION_INVITEE_ID, inviteeId);
		simpleJdbcTemplate.update(DELETE_INVITATIONS_BY_TEAM_AND_INVITEE, param);
	}

}