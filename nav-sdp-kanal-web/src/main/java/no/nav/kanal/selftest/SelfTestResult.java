package no.nav.kanal.selftest;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;


@JsonIgnoreProperties(value={"status", "name", "message"})
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
public class SelfTestResult {
	
	public enum Status {OK, ERROR}
	
	private Status status;
	private String name;
	private String description;
	private String endpoint;
	private String message;
	private String errorMessage;
	private String stacktrace;
	private String reponseTime;
	
	public SelfTestResult(String endpoint, Status status, String name, String message, String errorMessage, String stacktrace, String responseTime, String description) {
		this.endpoint = endpoint;
		this.status = status;
		this.name = name;
		this.message = message;
		this.errorMessage = errorMessage;
		this.stacktrace = stacktrace;
		this.reponseTime = responseTime;
		this.description = description;
		
	}

	public Status getStatus() {
		return status;
	}
	public String getResult() {
		return (status==Status.OK?"0":"1");
	}

	public String getName() {
		return name;
	}
	
	public String getEndpoint() {
		return endpoint;
	}

	public String getMessage() {
		return message;
	}
	
	public String getErrorMessage() {
		return errorMessage;
	}
	
	public String getDescription() {
		return description;
	}
	
	public String getStacktrace() {
		return stacktrace;
	}

	public String getReponseTime() {
		return reponseTime;
	}

}
