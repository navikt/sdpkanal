package no.nav.kanal.crypto;

import java.io.File;

import no.nav.kanal.SdpKanalApplication;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;


public class SignatureValidatorTest {
	
	private static final String EXPECTED_SIGNATURE_VALUE = "Cert 1:[Subject DN:(serialNumber=984661185,CN=POSTEN NORGE AS TEST,O=POSTEN NORGE AS,C=NO) + Issuer DN:(CN=Buypass Class 3 Test4 CA 3,O=Buypass AS-983163327,C=NO)]";

	@BeforeClass
	public static void oneTimeSetup() {
		SdpKanalApplication.registerIAIKProvider();
		System.out.println("IAIK added as provider");
	}

	@Ignore
	@Test
	public void tesSBDSignatureValidator(){
		
		// TODO:
		/*SignatureValidator sv = new SignatureValidator();
		sv.setTrustStoreFile(new File());
		sv.setTrustStorePassword("changeit");
		sv.setCertificateRevocationChecking(false);
		sv.setupTruststore();
		
		String result = sv.verifySBDSignature("<ns3:StandardBusinessDocument xmlns:ns3=\"http://www.unece.org/cefact/namespaces/StandardBusinessDocumentHeader\" xmlns:ns9=\"http://begrep.difi.no/sdp/schema_v10\"><ns3:StandardBusinessDocumentHeader><ns3:HeaderVersion>1.0</ns3:HeaderVersion><ns3:Sender><ns3:Identifier Authority=\"urn:oasis:names:tc:ebcore:partyid-type:iso6523:9908\">9908:984661185</ns3:Identifier></ns3:Sender><ns3:Receiver><ns3:Identifier Authority=\"urn:oasis:names:tc:ebcore:partyid-type:iso6523:9908\">9908:889640782</ns3:Identifier></ns3:Receiver><ns3:DocumentIdentification><ns3:Standard>urn:no:difi:sdp:1.0</ns3:Standard><ns3:TypeVersion>1.0</ns3:TypeVersion><ns3:InstanceIdentifier>3647c071-2c99-434b-9787-5cc8ce5760cd</ns3:InstanceIdentifier><ns3:Type>feil</ns3:Type><ns3:CreationDateAndTime>2017-05-10T13:59:29.925+02:00</ns3:CreationDateAndTime></ns3:DocumentIdentification><ns3:BusinessScope><ns3:Scope><ns3:Type>ConversationId</ns3:Type><ns3:InstanceIdentifier>8fa1395e-25d1-468e-99c6-415113574325</ns3:InstanceIdentifier><ns3:Identifier>urn:no:difi:sdp:1.0</ns3:Identifier></ns3:Scope></ns3:BusinessScope></ns3:StandardBusinessDocumentHeader><ns9:feil><Signature xmlns=\"http://www.w3.org/2000/09/xmldsig#\"><SignedInfo><CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/><SignatureMethod Algorithm=\"http://www.w3.org/2001/04/xmldsig-more#rsa-sha256\"/><Reference URI=\"\"><Transforms><Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"/></Transforms><DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\"/><DigestValue>1sTEVvIIQUlRPqy0x0hr9naUEQ/wbosQOjFgmmPJ9/U=</DigestValue></Reference></SignedInfo><SignatureValue>nLt+eP7EJtBrirzQrn0QKq0Xdp6p5nEp9H0cBFAe1m9xc1jbIxbu9HYTAtflDPSFQDmU03dl2wD4MkM3kQsq1lhEw8hFV5uYcEbl+6MsoMlBmGL1LZ3mvm6OhewQtmzSWEbaOTMa7xTvRgMz2EPZ+wLyFCVmBx/Q6yzwFtddaAq5yE2MC36kg4CRjA9461OKaBXxD2+v3irOXSkxZIUL4TyOMkMWY9DOJ94S8bPGBJ8Kp1OvMOUMFc7aAQl1yvRUoiuts6yGm2pqLxFOtWila/REyOdRtxM7ghAnSYOQ/qUblg3z3m+Mu4OSGlZtWdi3prQ0iRoB158cMbANDJ0g+w==</SignatureValue><KeyInfo><X509Data><X509Certificate>MIIE9DCCA9ygAwIBAgILATeICpJlVUsWQIowDQYJKoZIhvcNAQELBQAwUTELMAkGA1UEBhMCTk8xHTAbBgNVBAoMFEJ1eXBhc3MgQVMtOTgzMTYzMzI3MSMwIQYDVQQDDBpCdXlwYXNzIENsYXNzIDMgVGVzdDQgQ0EgMzAeFw0xNzAzMjgwNzA0MzVaFw0yMDAzMjgyMjU5MDBaMFoxCzAJBgNVBAYTAk5PMRgwFgYDVQQKDA9QT1NURU4gTk9SR0UgQVMxHTAbBgNVBAMMFFBPU1RFTiBOT1JHRSBBUyBURVNUMRIwEAYDVQQFEwk5ODQ2NjExODUwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQC5xfhyZbBRB0StkKdZOgbAWJxQdLAhPRZ6DiLNj2BfvXWuwi89f5Gu9N13wVNSkcxC3kVnxxXmVJT+cvxyyOi6y2He+tstA1CS7LYPTKv0Xzk6SmWGWYederMj22L0C4jwDfVIlySB/Y/PLCRDCMewjlwG152GYeslQP4mwC6LjacqgNVvcwJLYAUrMIKzbtrnS+oPb2epLU8O/frQWOoSEufEaSA/rh6jLWfblkoir8No5aDsIhTd9ILMIOIJwDHdo29mGXjpmVQRBGRBLLDhDt0uXkYZIYDg3gRpCbr4vGmUvAy4VNiZLYFhsdkfUazDOzBAn+BIPssqj/Je5cfLAgMBAAGjggHCMIIBvjAJBgNVHRMEAjAAMB8GA1UdIwQYMBaAFD+u9XgLkqNwIDVfWvr3JKBSAfBBMB0GA1UdDgQWBBTlibBg2L61AZXa+GAJtElRDIpANjAOBgNVHQ8BAf8EBAMCBLAwFgYDVR0gBA8wDTALBglghEIBGgEAAwIwgbsGA1UdHwSBszCBsDA3oDWgM4YxaHR0cDovL2NybC50ZXN0NC5idXlwYXNzLm5vL2NybC9CUENsYXNzM1Q0Q0EzLmNybDB1oHOgcYZvbGRhcDovL2xkYXAudGVzdDQuYnV5cGFzcy5uby9kYz1CdXlwYXNzLGRjPU5PLENOPUJ1eXBhc3MlMjBDbGFzcyUyMDMlMjBUZXN0NCUyMENBJTIwMz9jZXJ0aWZpY2F0ZVJldm9jYXRpb25MaXN0MIGKBggrBgEFBQcBAQR+MHwwOwYIKwYBBQUHMAGGL2h0dHA6Ly9vY3NwLnRlc3Q0LmJ1eXBhc3Mubm8vb2NzcC9CUENsYXNzM1Q0Q0EzMD0GCCsGAQUFBzAChjFodHRwOi8vY3J0LnRlc3Q0LmJ1eXBhc3Mubm8vY3J0L0JQQ2xhc3MzVDRDQTMuY2VyMA0GCSqGSIb3DQEBCwUAA4IBAQBeGZyhAOQ0HsTuVIF9r+8E0whlig1N4AufFRGfIJdTu7lulMF6IZ79hDqR4Fe+66/fjeBwCx3M9ulnjOglUcJLTmn9Fp1X/GwDs8HTP0h/uVByFnweSkbF1oDqea+/lmOnULwMaCLG+ibzvd5igG9QRWoc3xQJE0XNajj2SdlKmN8+o3TxhOdLfiDo5BoqF+XffwNtVR/QsLjaCiyM9rJXfetFRwH7aB/Slk9ygICXCdPP/kQz5T9dE5Lzi0bVe2OpiYUD6ZC38W0MDkmvEJv0v5heFxOsvcSfjZP1j8asg4EASiiiWMoQ6UI0kLtd8MJtTJRdQoxhXEiQdaz97AFj</X509Certificate></X509Data></KeyInfo></Signature><ns9:tidspunkt>2017-05-10T13:59:29.925+02:00</ns9:tidspunkt><ns9:feiltype>KLIENT</ns9:feiltype><ns9:detaljer>Kan ikke dekryptere dokumentpakke - brukes feil nøkkel?</ns9:detaljer></ns9:feil></ns3:StandardBusinessDocument>");
		Assert.assertEquals("Signature should be right", EXPECTED_SIGNATURE_VALUE, result);*/
	}
	
}
