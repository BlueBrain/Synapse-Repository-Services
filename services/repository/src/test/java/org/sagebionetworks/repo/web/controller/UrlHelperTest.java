package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.Code;
import org.sagebionetworks.repo.model.Data;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Preview;
import org.sagebionetworks.repo.model.Study;
import org.sagebionetworks.repo.model.Versionable;
import org.sagebionetworks.repo.web.UrlHelpers;

/**
 * Test for the URL helper
 * @author jmhill
 *
 */
public class UrlHelperTest {
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetUrlPrefixFromRequestNullRequst(){
		String url = UrlHelpers.getUrlPrefixFromRequest(null);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testGetUrlPrefixFromRequestBothNull(){
		HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
		when(mockRequest.getContextPath()).thenReturn(null);
		when(mockRequest.getServletPath()).thenReturn(null);
		String url = UrlHelpers.getUrlPrefixFromRequest(mockRequest);
	}
	
	@Test
	public void testGetUrlPrefixFromRequestContextNull(){
		HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
		when(mockRequest.getContextPath()).thenReturn(null);
		when(mockRequest.getServletPath()).thenReturn("/repo/v1");
		String url = UrlHelpers.getUrlPrefixFromRequest(mockRequest);
		assertEquals("/repo/v1", url);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetUrlPrefixFromRequestPathNull(){
		HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
		when(mockRequest.getContextPath()).thenReturn("http://localhost:8080");
		when(mockRequest.getServletPath()).thenReturn(null);
		String url = UrlHelpers.getUrlPrefixFromRequest(mockRequest);
		assertEquals("http://localhost:8080", url);
	}
	
	@Test 
	public void testGetUrlPrefixFromRequestBothNotNull(){
		HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
		when(mockRequest.getContextPath()).thenReturn("http://localhost:8080");
		when(mockRequest.getServletPath()).thenReturn("/repo/v1");
		String url = UrlHelpers.getUrlPrefixFromRequest(mockRequest);
		assertEquals("http://localhost:8080/repo/v1", url);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testsetEntityUriNullId(){
		UrlHelpers.createEntityUri(null, Study.class, "http://localhost:8080/repo/v1");
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testsetEntityUriNullClass(){
		UrlHelpers.createEntityUri("12", null, "http://localhost:8080/repo/v1");
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testsetEntityUriNullPrefix(){
		UrlHelpers.createEntityUri("12", Data.class, null);
	}
	
	@Test 
	public void testsetEntityUriAllTypes(){
		EntityType[] array = EntityType.values();
		String uriPrefix = "/repo/v1";
		String id = "123";
		for(EntityType type: array){
			String expectedUri = uriPrefix+UrlHelpers.ENTITY+"/"+id;
			String uri = UrlHelpers.createEntityUri(id, type.getClassForType(), uriPrefix);
			assertEquals(expectedUri, uri);
		}
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testSetBaseUriForEntityNullEntity(){
		HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
		when(mockRequest.getContextPath()).thenReturn("http://localhost:8080");
		when(mockRequest.getServletPath()).thenReturn("/repo/v1");
		UrlHelpers.setBaseUriForEntity(null, mockRequest);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testSetBaseUriForEntityNullRequest(){
		Study mockDs = Mockito.mock(Study.class);
		when(mockDs.getId()).thenReturn("123");
		UrlHelpers.setBaseUriForEntity(mockDs, null);
	}
	
	@Test
	public void testSetBaseUriForEntity(){
		HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
		when(mockRequest.getContextPath()).thenReturn("http://localhost:8080");
		when(mockRequest.getServletPath()).thenReturn("/repo/v1");
		Study ds = new Study();
		ds.setId("456");
		UrlHelpers.setBaseUriForEntity(ds, mockRequest);
		String expectedUri = "http://localhost:8080/repo/v1/entity/456";
		assertEquals(expectedUri, ds.getUri());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testSetAllEntityUrlsNull(){
		UrlHelpers.setAllEntityUrls(null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testSetAllEntityNullUri(){
		Preview preview = new Preview();
		preview.setUri(null);
		UrlHelpers.setAllEntityUrls(preview);
	}
	
	@Test
	public void testSetAllEntityUrls(){
		Preview preview = new Preview();
		// Make sure the preview has a uri
		String baseUri = "/repo/v1/entity/42";
		preview.setUri(baseUri);
		UrlHelpers.setAllEntityUrls(preview);
		assertEquals(baseUri+UrlHelpers.ACL, preview.getAccessControlList());
		assertEquals(baseUri+UrlHelpers.ANNOTATIONS, preview.getAnnotations());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testSetVersionableNullUri(){
		Study dataset = new Study();
		dataset.setUri(null);
		UrlHelpers.setVersionableUrl(dataset);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testSetVersionableNullVersionNumber(){
		Data layer = new Data();
		String baseUri = "/repo/v1"+UrlHelpers.ENTITY+"/42";
		layer.setUri(baseUri);
		// set the version number to be null
		layer.setVersionNumber(null);
		UrlHelpers.setVersionableUrl(layer);
	}
	
	@Test
	public void testSetVersionable(){
		Data layer = new Data();
		layer.setVersionNumber(new Long(12));
		// Make sure the layer has a uri
		String baseUri = "/repo/v1"+UrlHelpers.ENTITY+"/42";
		layer.setUri(baseUri);
		UrlHelpers.setVersionableUrl(layer);
		assertEquals(baseUri+UrlHelpers.VERSION, layer.getVersions());
		assertEquals(baseUri+UrlHelpers.VERSION+"/12", layer.getVersionUrl());
	}
	
	@Test
	public void testSetAllUrlsForEntity() throws InstantiationException, IllegalAccessException{
		HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
		when(mockRequest.getContextPath()).thenReturn(null);
		String base = "/repo/v1";
		String id = "56";
		when(mockRequest.getServletPath()).thenReturn(base);
		// Test each type
		EntityType[] array = EntityType.values();
		for(EntityType type: array){
			Entity entity = type.getClassForType().newInstance();
			entity.setId(id);
			if(entity instanceof Versionable){
				Versionable able = (Versionable) entity;
				// Make sure it has a version number
				able.setVersionNumber(43l);
			}
			UrlHelpers.setAllUrlsForEntity(entity, mockRequest);
			String expectedBase = base+UrlHelpers.ENTITY+"/"+id;
			assertEquals(expectedBase, entity.getUri());
			String expected = expectedBase+UrlHelpers.ANNOTATIONS;
			assertEquals(expected, entity.getAnnotations());
			expected =  expectedBase+UrlHelpers.ACL;
			assertEquals(expected, entity.getAccessControlList());
			// Versionable
			if(entity instanceof Versionable){
				Versionable able = (Versionable) entity;
				// Make sure it has a version number
				expected = expectedBase+UrlHelpers.VERSION;
				assertEquals(expected, able.getVersions());
				expected = expectedBase+UrlHelpers.VERSION+"/43";
				assertEquals(expected, able.getVersionUrl());
			}
		}
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateAllUrlsNullBase(){
		Data layer = new Data();
		UrlHelpers.validateAllUrls(layer);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateAllNullAnnos(){
		Data layer = new Data();
		layer.setUri("repo/v1/layer/33");
		UrlHelpers.setAllEntityUrls(layer);
		layer.setAnnotations(null);
		UrlHelpers.validateAllUrls(layer);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateAllNullACL(){
		Data layer = new Data();
		layer.setUri("repo/v1/layer/33");
		UrlHelpers.setAllEntityUrls(layer);
		layer.setAccessControlList(null);
		UrlHelpers.validateAllUrls(layer);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateAllNullLocations(){
		Data layer = new Data();
		layer.setUri("repo/v1/layer/33");
		UrlHelpers.setAllEntityUrls(layer);
		layer.setLocations(null);
		UrlHelpers.validateAllUrls(layer);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateAllNullVersions(){
		Code code = new Code();
		code.setVersionNumber(45l);
		code.setUri("repo/v1/code/33");
		UrlHelpers.setAllEntityUrls(code);
		code.setVersions(null);
		UrlHelpers.validateAllUrls(code);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateAllNullVersionUrl(){
		Code code = new Code();
		code.setVersionNumber(1l);
		code.setUri("repo/v1/code/33");
		UrlHelpers.setAllEntityUrls(code);
		code.setVersionUrl(null);
		UrlHelpers.validateAllUrls(code);
	}

	@Test
	public void testCreateACLRedirectURL(){
		HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
		when(mockRequest.getContextPath()).thenReturn("http://localhost:8080");
		when(mockRequest.getServletPath()).thenReturn("/repo/v1");
		String redirectURL = UrlHelpers.createACLRedirectURL(mockRequest, "45");
		assertEquals("http://localhost:8080/repo/v1/entity/45/acl", redirectURL);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateACLRedirectURLNullRequst(){
		String redirectURL = UrlHelpers.createACLRedirectURL(null, "45");
	}
	
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateACLRedirectURLNullId(){
		HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
		when(mockRequest.getContextPath()).thenReturn("http://localhost:8080");
		when(mockRequest.getServletPath()).thenReturn("/repo/v1");
		String redirectURL = UrlHelpers.createACLRedirectURL(mockRequest, null);
	}

}
