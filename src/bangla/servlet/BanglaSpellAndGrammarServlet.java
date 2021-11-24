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
import bangla.log.InputOutputLogDAO;
import config.GlobalConfigConstants;
import config.GlobalConfigDTO;
import config.GlobalConfigurationRepository;
import repository.RepositoryManager;
import bangla.dto.*;


@WebServlet("/bangla_spell_and_grammar")
@MultipartConfig
public class BanglaSpellAndGrammarServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;


	static Logger logger = Logger.getLogger(BanglaSpellAndGrammarServlet.class);


	public BanglaSpellAndGrammarServlet() {
		super();
	}

	public void init(ServletConfig config) throws ServletException {
		System.out.println("Starting Bangla Spell & Grammar server: init(ServletConfig config) called");
		SpellAndGrammarChecker.getInstance().startupCode();
		logger.debug("Starting Bangla Spell & Grammar Error Checker init() called at " + new java.util.Date());
	}


	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {


			long startTime = System.currentTimeMillis();

			String text_data = request.getParameter("content");
			String maxSuggestion = request.getParameter("MaxSuggestionCount");
			String client = request.getParameter("Client");
			String version = request.getParameter("Version");
			String ipAddress = request.getRemoteAddr();
			logger.debug("---------------------------------------------------------------------\n");
			logger.debug("Received Post Request from Client: " + client + " With Version: " + version + " And MaxSuggestionCount: " + maxSuggestion + " From: " + ipAddress + " at: " + new java.util.Date());

			if (text_data == null || text_data.length() == 0) {
				logger.debug("End of request processing: Input is Empty");
				response.sendError(400, "Input request is empty");
				return;
			}


			logger.debug("ContentLength:" + text_data.length());
			logger.debug("Content:" + text_data);

			int maxSuggestionLimit = 0;

			if(maxSuggestion == null || maxSuggestion.length() == 0) {
				GlobalConfigDTO gcDTO = GlobalConfigurationRepository.getGlobalConfigDTOByID(GlobalConfigConstants.MAX_SUGGESTION_LIMIT);
				if (gcDTO != null) {
					maxSuggestionLimit = Integer.parseInt(gcDTO.value);
				}
			}
			else {
				maxSuggestionLimit = Integer.parseInt(maxSuggestion);
			}

			DocumentDTO documentDTO = new DocumentDTO(text_data, maxSuggestionLimit);

			SpellAndGrammarChecker.getInstance().checkDocument(documentDTO);

			documentDTO.setLogTag(InputOutputLogDAO.getInstance().getNextLogID());

			String finalResultStr = documentDTO.toJson();

			if (finalResultStr == null) {
				logger.debug("End of request processing: Input is non-interpretable");
				response.sendError(400, "could not understand input request text");
				return;
			}


			InputOutputLogDAO.getInstance().add(documentDTO.logID, ipAddress, text_data, finalResultStr, request.getSession().getId(), 1, documentDTO.unknownWordCounter);
			response.setContentType("application/json;charset=UTF-8");
			response.setHeader("Access-Control-Allow-Origin", "*");

			PrintWriter out = response.getWriter();
			out.print(finalResultStr);
			out.flush();
			logger.debug("Sending Response\n");
			logger.debug(finalResultStr);
			logger.debug("\nEnd of request processing.");
			logger.debug("Time to process:" + (System.currentTimeMillis() - startTime) + " MilliSeconds");

		} catch (Exception ex) {
			logger.error("Error generated while processing this request", ex);
			response.sendError(500, "Internal Server Error");

		}
	}

	public void destroy() {
		logger.debug("destroy() called");
		InputOutputLogDAO.getInstance().shutdown();
		SpellAndGrammarChecker.getInstance().shutdown();
		RepositoryManager.getInstance().shutDown();
	}
}

