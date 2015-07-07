package com.redhat.gss.redhat_support_lib.api;

import static org.junit.Assert.*;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Calendar;
import java.util.List;

import org.junit.Test;

import com.redhat.gss.redhat_support_lib.errors.RequestException;
import com.redhat.gss.redhat_support_lib.infrastructure.Attachments;
import com.redhat.gss.redhat_support_lib.parsers.ArticleType;
import com.redhat.gss.redhat_support_lib.parsers.AttachmentType;
import com.redhat.gss.redhat_support_lib.parsers.CaseType;
import com.redhat.gss.redhat_support_lib.parsers.CommentType;
import com.redhat.gss.redhat_support_lib.parsers.EntitlementType;
import com.redhat.gss.redhat_support_lib.parsers.LinkType;
import com.redhat.gss.redhat_support_lib.parsers.ProductType;
import com.redhat.gss.redhat_support_lib.parsers.SearchResultType;
import com.redhat.gss.redhat_support_lib.parsers.SolutionType;

public class APITest extends API {

	public APITest() throws Exception {
		super("src/test/java/rhsl.config");
	}
	
	@Test
	public void searchTest() throws Exception {
		String[] searchTerms = {"rhev"};
		List<SearchResultType> searchResults = getSearch().search(searchTerms);
		assertTrue(searchResults.get(0) instanceof SearchResultType );
	}

	@Test
	public void getSolutionTest() throws Exception {
		SolutionType retrievedSol = getSolutions().get("330903");
		assertEquals("330903", retrievedSol.getId());
	}

	@Test
	public void listSolutionTest() throws Exception {
		String[] args = { "Test" };
		List<SolutionType> sols = getSolutions().list(args, null);
		assertNotNull(sols.get(0));
	}

	@Test
	public void getArticleTest() throws Exception {
		ArticleType retrievedArticle = getArticles().get("546543");
		assertEquals("546543", retrievedArticle.getId());
	}
	
	public ArticleType addArticle(String abstractTxt, String title) {
		ArticleType art = new ArticleType();
		art.setAbstract(abstractTxt);
		art.setTitle(title);
		try {
			art = getArticles().add(art);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return art;
	}

	
	@Test public void listArticleTest() throws MalformedURLException, RequestException { 
		String[] args = {"keyword=NFS"};
		List<ArticleType> arts = getArticles().list(args, null);
		assertEquals(arts.size(), 50); 
	}

	@Test
	public void getCaseTest() throws Exception {
		CaseType cas = this.addCase();
		CaseType retrievedCase = getCases().get(cas.getCaseNumber());
		assertEquals(cas.getCaseNumber(), retrievedCase.getCaseNumber());
	}

	@Test
	public void listCaseTest() throws Exception {
		List<CaseType> cases = getCases().list(null, true, false, null, null,
				null, null, null, null);
		assertNotNull(cases.get(0).getOwner());
	}

	@Test
	public void AddCaseTest() throws Exception {
		CaseType cas = addCase();
		assertNotNull(cas.getCaseNumber());

		// Close case
		cas.setStatus("Closed");
		cas = getCases().update(cas);
		assertEquals("Closed", cas.getStatus());
	}
	
	/**
	 * Creates new Case
	 * 
	 * @return new case with test values
	 */
	private CaseType addCase() {
		CaseType cas = new CaseType();
		cas.setProduct("Other");
		cas.setVersion("Unknown");
		cas.setSummary("This is a test");
		cas.setDescription("This is a test");
		cas.setStatus("Waiting on Red Hat");
		try {
			cas = getCases().add(cas);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return cas;
	}

	@Test
	public void UpdateCaseTest() throws Exception {
		CaseType cas = this.addCase();		
		cas.setStatus("Closed");
		cas = getCases().update(cas);
		assertEquals("Closed", cas.getStatus());
	}

	@Test
	public void listProductsTest() throws Exception {
		List<ProductType> prods = getProducts().list(null);
		assertNotNull(prods.get(0));
	}

	@Test
	public void getVersionsTest() throws Exception {
		List<String> versions = getProducts().getVersions("FeedHenry");
		assertEquals(versions.get(0), "3");
	}

	@Test
	public void listCommentsTest() throws Exception {
		CaseType cas = this.addCase();
		this.addComment(cas.getCaseNumber(), "Test Comment");
		List<CommentType> comments = getComments().list(cas.getCaseNumber(), null, null,
				null);
		assertEquals(comments.size(),1);
	}

	@Test
	public void getCommentTest() throws Exception {
		String commentText = "Comment Longgggggggggggggggggggggggggggggggggggggggggggg";
		CaseType caseType = this.addCase();
		CommentType newComment = this.addComment(caseType.getCaseNumber(), commentText);
		CommentType comment = getComments().get(caseType.getCaseNumber(), newComment.getId());
		assertEquals(comment.getText(), commentText);
	}

	@Test
	public void AddCommentTest() {
		CaseType cas = this.addCase();
		CommentType comment = addComment(cas.getCaseNumber(), 
				"Test Comment from Java APITest");
		assertNotNull(comment.getViewUri());
	}
	
	private CommentType addComment(String caseId, String text) {
		CommentType comment = new CommentType();
		comment.setCaseNumber(caseId);
		comment.setText(text);
		try {
			comment = getComments().add(comment);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return comment;
	}

	@Test
	public void listEntitlementsTest() throws Exception {
		String product = "L1-L3: Red Hat Enterprise Linux NFR (Up to 20 guests)";
		List<EntitlementType> entitlements = getEntitlements().list(false, product, null);
		assertNotNull(entitlements.get(0));
	}

	@Test
	public void diagnoseStrProblemsTest() throws Exception {
		List<LinkType> problems = getProblems().diagnoseStr("error");
		assertNotNull(problems.get(0));
	}

	@Test
	public void diagnoseFileProblemsTest() throws Exception {
		List<LinkType> problems = getProblems().diagnoseFile(
				"src/test/java/test.txt");
		assertNotNull(problems.get(0));
	}

	@Test
	public void listAttachmentsTest() throws Exception {
		CaseType cas = this.addCase();
		assertEquals(getAttachments().list(cas.getCaseNumber(), null, null, null).size(), 0);
		
		getAttachments().add(cas.getCaseNumber(), true, "src/test/java/test.txt", "Test Attachment");
		
		List<AttachmentType> attachments = getAttachments().list(cas.getCaseNumber(), null,
				null, null);
		assertEquals(attachments.size(), 1);
	}

	@Test
	public void AddAndDeleteAttachmentTest() throws Exception {
		CaseType cas = this.addCase();
		
		assertEquals(getAttachments().list(cas.getCaseNumber(), null, null, null).size(), 0);
				
		String location = getAttachments().add(cas.getCaseNumber(), true,
				"src/test/java/test.txt", 
				"This is a test");
		assertNotNull(location);
		String[] parsedURI = location.split("/");
		
		Boolean success = getAttachments().delete(cas.getCaseNumber(),
				parsedURI[parsedURI.length - 1]);
		assertTrue(success);
	}
	
	@Test
	public void caseLifecycleTest() throws Exception {
		// Open case
		CaseType cas = addCase();
		String caseNumber = cas.getCaseNumber();
		assertNotNull(caseNumber);
		assertEquals("Waiting on Red Hat", cas.getStatus());
		
		// Add an attachment
		String location = getAttachments().add(caseNumber, true, "src/test/java/test.txt", 
				"This is a test");
		assertNotNull(location);
		
		// Add a comment
		CommentType comment = addComment(cas.getCaseNumber(), "Test Comment from Java APITest");
		assertNotNull(comment.getViewUri());

		// Close case
		cas.setStatus("Closed");
		cas = getCases().update(cas);
		assertEquals("Closed", cas.getStatus());
	}
}
