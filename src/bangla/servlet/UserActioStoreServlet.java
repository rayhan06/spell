package bangla.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.*;
import org.apache.log4j.Logger;

import bangla.SpellAndGrammarChecker;
import bangla.log.*;

@WebServlet("/user_action_store")
@MultipartConfig
public class UserActioStoreServlet extends HttpServlet 
{
	private static final long serialVersionUID = 1L;
	public static Logger logger = Logger.getLogger(UserActioStoreServlet.class);
	SpellAndGrammarChecker spellAndGrammarChecker;
	
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{
    	InputOutputLogDetailDAO theDao = new InputOutputLogDetailDAO();
    	
    	long logid = Long.parseLong(request.getParameter("logid")); 
    	String action = request.getParameter("action");
    	String content = request.getParameter("content");
    	String newValue = request.getParameter("newValue");
    	
    	String responseString = "OK";

    	try {
			theDao.add(logid, action, content, newValue);
		} catch (Exception e) {
			responseString = e.getMessage();
		}
    	
		response.setContentType("application/json;charset=UTF-8");
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
		
		PrintWriter out = response.getWriter();
		out.print(responseString);
		out.flush();
		logger.debug("Sending Response");
		logger.debug("End of request processing." );		
	}
	
		
	@Override
	  protected void doOptions(HttpServletRequest req, HttpServletResponse resp)
	          throws ServletException, IOException {
	      setAccessControlHeaders(resp);
	      resp.setStatus(HttpServletResponse.SC_OK);
	  }

	  private void setAccessControlHeaders(HttpServletResponse resp) {
	      resp.setHeader("Access-Control-Allow-Origin", "*");
	      resp.setHeader("Access-Control-Allow-Headers", "x-requested-with");
	      resp.setHeader("Access-Control-Allow-Methods"," GET, POST, OPTIONS");
		  //resp.setHeader("Access-Control-Allow-Headers","Content-Type");
		  //resp.setHeader("Content-Type", "application/json");
	  }	
}

