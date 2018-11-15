package no.nav.kanal;

public final class KanalConstants {

	public static final String EBMS_PMODE_EVENTID_DIGITAL_PUSH = "SendDigitalPost";
	public static final String EBMS_PMODE_EVENTID_FYSISK_PUSH = "SendFysiskPost";
	public static final String EBMS_PMODE_EVENTID_PULL = "HentKvittering";
	
	public static final String SDP_MPC_NORMAL = "urn:normal";
	public static final String SDP_MPC_PRIORITERT = "urn:prioritert";
	
	public static final String CAMEL_HEADER_TEMP_DIRECTORY = "TEMP_DIRECTORY_PATH";
	public static final String CAMEL_HEADER_DOKUMENTPAKKE = "DOKUMENTPAKKE";
	public static final String CAMEL_HEADER_STANDARD_BUSINESS_DOCUMENT = "SIGNED_STANDARD_BUSINESS_DOCUMENT_FILE_PATH";
	public static final String CAMEL_HEADER_MPC = "MPC";
	public static final String CAMEL_HEADER_LOOP_INDICATOR_NORMAL = "LOOP_INDICATOR_NORMAL";
	public static final String CAMEL_HEADER_LOOP_INDICATOR_PRIORITERT = "LOOP_INDICATOR_PRIORITERT";
	public static final String CAMEL_HEADER_MESSAGE_TO_ACK = "MESSAGE_TO_ACK";
	public static final String CAMEL_HEADER_LEGAL_ARCHIVE_LOG_ID = "LEGAL_ARCHIVE_LOG_ID";
	public static final String CAMEL_HEADER_TYPE = "type";
	public static final String CAMEL_HEADER_TYPE_FYSISK_POST = "fysisk";
	public static final String CAMEL_HEADER_TYPE_DIGITAL_POST = "digital";
	public static final String CAMEL_HEADER_BOQ_MESSAGE = "_exceptionMessage";
	public static final String CAMEL_PROPERTY_ERROR_TO_PROPAGATE = "FEILMELDING";
	
	public static final String LEGAL_ARCHIVE_LOG_ACTION_FYSISK = "FormidleFysiskPost";
	public static final String LEGAL_ARCHIVE_LOG_ACTION_DIGITAL = "FormidleDigitalPost";
	
	public static final int  SYSTEM_BLOCK_READ_SIZE = 4096;
	
	public static final String DOKUMENTPAKKE_MANIFEST_FILE_NAME = "manifest.xml";
	
	
	
	
	
}
