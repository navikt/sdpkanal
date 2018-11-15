package no.nav.kanal.metrics;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.context.support.WebApplicationContextUtils;

@WebServlet("/internal/metric")
public class MetricsServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		Metrics metrics = (Metrics) WebApplicationContextUtils.getWebApplicationContext(getServletContext()).getBean("sdpkanalMetrics");
		String metricsJson = metrics.getMetrics();
		
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");
		PrintWriter out = resp.getWriter();
		out.println(metricsJson);
		resp.setStatus(HttpServletResponse.SC_OK);
		out.flush();
		out.close();
	}

}
