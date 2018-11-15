package no.nav.kanal.selftest;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Servlet implementation class SelfTest
 */
@WebServlet("/internal/selftest")
public class SelfTestServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
     * Default constructor. 
     */
    public SelfTestServlet() {
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// get ref to selftest-bean and refresh results
		SelfTest st = (SelfTest) WebApplicationContextUtils.getWebApplicationContext(getServletContext()).getBean("selftest");  
		st.refresh(request);
		String accept = request.getHeader(HttpHeaders.ACCEPT);
		if(accept != null && accept.contains("application/json")) {
			outputJson(response, st);
		} else {
			outputHtml(response, st);
		}
		
	}

	private void outputJson(HttpServletResponse response, SelfTest st) throws IOException {
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		PrintWriter out = response.getWriter();
		ObjectMapper mapper = new ObjectMapper();
		String json = mapper.writeValueAsString(st.getResults());
		out.println("{\"application\": \"" + getAppName() + "\"," +
					" \"version\": \""+ getAppversion() + "\"," +
					" \"timestamp\": \""+ getCurrentTimeStamp("yyyy-MM-dd'T'HH:mm:ss.SSSZ") + "\"," +
					" \"aggregate_result\": \""+ (st.getReturnCode()==200?"0":"1") + "\"," +
					"\"checks\":" + json +
					"}");
		response.setStatus(st.getReturnCode());
		out.flush();
		out.close();
		
	}

	private void outputHtml(HttpServletResponse response, SelfTest st)
			throws IOException {
		// output results
		response.setContentType("text/html");
		response.setCharacterEncoding("UTF-8");
		PrintWriter out = response.getWriter();
		out.println("<!DOCTYPE html>");		
		out.println("<html>");
		out.println("<head><title>sdpkanal selftest</title></head>");
		out.println("<body>");		
		out.println("<h1>Service status: " + (st.getReturnCode()==200?"OK":"ERROR") +  "</h1>");
		out.println("<h3>sdp-kanal " + getAppversion() + "</h3>");
		out.println("<table border='1' style='width:100%'>");
		out.println("<th>Status</th><th>Ressurs</th><th>Melding</th><th>Responstid</th>");

		List<SelfTestResult> results = st.getResults();
		for(int i=0;i<results.size();i++) {
			out.println("<tr>");
			out.println("<td>" +  results.get(i).getStatus() + "</td>");
			out.println("<td>" +  results.get(i).getName() + "</td>");
			out.println("<td>" +  results.get(i).getMessage() + "</td>");
			out.println("<td>" +  results.get(i).getReponseTime() + "</td>");
			out.println("</tr>");
		}

		out.println("</table>");
		out.println("<br>Siden generert: " + getCurrentTimeStamp("yyyy-MM-dd HH:mm:ss"));
		out.println("<h6 id=\"driftMelding\">");
		StringBuffer driftsMelding = new StringBuffer();
		for(int i=0;i<results.size();i++) {
			if(results.get(i).getStatus().equals(SelfTestResult.Status.ERROR)) {
				driftsMelding.append(results.get(i).getName()).append(',');
			}
		}
		out.println(driftsMelding.toString().replaceAll(",$", ""));
		out.println("</h6>");
		out.println("</body>");
		out.println("</html>");
		response.setStatus(st.getReturnCode());
		out.flush();
		out.close();
	}

	private String getAppName() {
		Properties props = new Properties();
		try {
			props.load(getServletContext().getResourceAsStream("/META-INF/MANIFEST.MF"));
		} catch (IOException e) {
			return "UNKNOWN VERSION";
		}
		return props.getProperty("Implementation-Title");
	}
	
	private String getAppversion() {
		Properties props = new Properties();
		try {
			props.load(getServletContext().getResourceAsStream("/META-INF/MANIFEST.MF"));
		} catch (IOException e) {
			return "UNKNOWN VERSION";
		}
		return props.getProperty("Implementation-Version");
	}

	private String getCurrentTimeStamp(String format) {
		SimpleDateFormat sdfDate = new SimpleDateFormat(format);
		Date now = new Date();
		String strDate = sdfDate.format(now);
		return strDate;
	}

}
