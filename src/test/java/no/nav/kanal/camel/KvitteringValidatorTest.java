package no.nav.kanal.camel;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.CamelSpringJUnit4ClassRunner;
import org.apache.camel.test.spring.MockEndpoints;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;

import no.nav.kanal.KanalConstants;

@RunWith(CamelSpringJUnit4ClassRunner.class)
@ContextConfiguration({"src/test/resources/applicationContext-kvitteringValidator.xml"})
@MockEndpoints("log:*")
public class KvitteringValidatorTest {
	
	private String inputBody = "<ns3:StandardBusinessDocument xmlns:ns3=\"http://www.unece.org/cefact/namespaces/StandardBusinessDocumentHeader\" xmlns:ns9=\"http://begrep.difi.no/sdp/schema_v10\"><ns3:StandardBusinessDocumentHeader><ns3:HeaderVersion>1.0</ns3:HeaderVersion><ns3:Sender><ns3:Identifier Authority=\"urn:oasis:names:tc:ebcore:partyid-type:iso6523:9908\">9908:984661185</ns3:Identifier></ns3:Sender><ns3:Receiver><ns3:Identifier Authority=\"urn:oasis:names:tc:ebcore:partyid-type:iso6523:9908\">9908:889640782</ns3:Identifier></ns3:Receiver><ns3:DocumentIdentification><ns3:Standard>urn:no:difi:sdp:1.0</ns3:Standard><ns3:TypeVersion>1.0</ns3:TypeVersion><ns3:InstanceIdentifier>3647c071-2c99-434b-9787-5cc8ce5760cd</ns3:InstanceIdentifier><ns3:Type>feil</ns3:Type><ns3:CreationDateAndTime>2017-05-10T13:59:29.925+02:00</ns3:CreationDateAndTime></ns3:DocumentIdentification><ns3:BusinessScope><ns3:Scope><ns3:Type>ConversationId</ns3:Type><ns3:InstanceIdentifier>8fa1395e-25d1-468e-99c6-415113574325</ns3:InstanceIdentifier><ns3:Identifier>urn:no:difi:sdp:1.0</ns3:Identifier></ns3:Scope></ns3:BusinessScope></ns3:StandardBusinessDocumentHeader><ns9:feil><Signature xmlns=\"http://www.w3.org/2000/09/xmldsig#\"><SignedInfo><CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/><SignatureMethod Algorithm=\"http://www.w3.org/2001/04/xmldsig-more#rsa-sha256\"/><Reference URI=\"\"><Transforms><Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"/></Transforms><DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\"/><DigestValue>1sTEVvIIQUlRPqy0x0hr9naUEQ/wbosQOjFgmmPJ9/U=</DigestValue></Reference></SignedInfo><SignatureValue>nLt+eP7EJtBrirzQrn0QKq0Xdp6p5nEp9H0cBFAe1m9xc1jbIxbu9HYTAtflDPSFQDmU03dl2wD4MkM3kQsq1lhEw8hFV5uYcEbl+6MsoMlBmGL1LZ3mvm6OhewQtmzSWEbaOTMa7xTvRgMz2EPZ+wLyFCVmBx/Q6yzwFtddaAq5yE2MC36kg4CRjA9461OKaBXxD2+v3irOXSkxZIUL4TyOMkMWY9DOJ94S8bPGBJ8Kp1OvMOUMFc7aAQl1yvRUoiuts6yGm2pqLxFOtWila/REyOdRtxM7ghAnSYOQ/qUblg3z3m+Mu4OSGlZtWdi3prQ0iRoB158cMbANDJ0g+w==</SignatureValue><KeyInfo><X509Data><X509Certificate>MIIE9DCCA9ygAwIBAgILATeICpJlVUsWQIowDQYJKoZIhvcNAQELBQAwUTELMAkGA1UEBhMCTk8xHTAbBgNVBAoMFEJ1eXBhc3MgQVMtOTgzMTYzMzI3MSMwIQYDVQQDDBpCdXlwYXNzIENsYXNzIDMgVGVzdDQgQ0EgMzAeFw0xNzAzMjgwNzA0MzVaFw0yMDAzMjgyMjU5MDBaMFoxCzAJBgNVBAYTAk5PMRgwFgYDVQQKDA9QT1NURU4gTk9SR0UgQVMxHTAbBgNVBAMMFFBPU1RFTiBOT1JHRSBBUyBURVNUMRIwEAYDVQQFEwk5ODQ2NjExODUwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQC5xfhyZbBRB0StkKdZOgbAWJxQdLAhPRZ6DiLNj2BfvXWuwi89f5Gu9N13wVNSkcxC3kVnxxXmVJT+cvxyyOi6y2He+tstA1CS7LYPTKv0Xzk6SmWGWYederMj22L0C4jwDfVIlySB/Y/PLCRDCMewjlwG152GYeslQP4mwC6LjacqgNVvcwJLYAUrMIKzbtrnS+oPb2epLU8O/frQWOoSEufEaSA/rh6jLWfblkoir8No5aDsIhTd9ILMIOIJwDHdo29mGXjpmVQRBGRBLLDhDt0uXkYZIYDg3gRpCbr4vGmUvAy4VNiZLYFhsdkfUazDOzBAn+BIPssqj/Je5cfLAgMBAAGjggHCMIIBvjAJBgNVHRMEAjAAMB8GA1UdIwQYMBaAFD+u9XgLkqNwIDVfWvr3JKBSAfBBMB0GA1UdDgQWBBTlibBg2L61AZXa+GAJtElRDIpANjAOBgNVHQ8BAf8EBAMCBLAwFgYDVR0gBA8wDTALBglghEIBGgEAAwIwgbsGA1UdHwSBszCBsDA3oDWgM4YxaHR0cDovL2NybC50ZXN0NC5idXlwYXNzLm5vL2NybC9CUENsYXNzM1Q0Q0EzLmNybDB1oHOgcYZvbGRhcDovL2xkYXAudGVzdDQuYnV5cGFzcy5uby9kYz1CdXlwYXNzLGRjPU5PLENOPUJ1eXBhc3MlMjBDbGFzcyUyMDMlMjBUZXN0NCUyMENBJTIwMz9jZXJ0aWZpY2F0ZVJldm9jYXRpb25MaXN0MIGKBggrBgEFBQcBAQR+MHwwOwYIKwYBBQUHMAGGL2h0dHA6Ly9vY3NwLnRlc3Q0LmJ1eXBhc3Mubm8vb2NzcC9CUENsYXNzM1Q0Q0EzMD0GCCsGAQUFBzAChjFodHRwOi8vY3J0LnRlc3Q0LmJ1eXBhc3Mubm8vY3J0L0JQQ2xhc3MzVDRDQTMuY2VyMA0GCSqGSIb3DQEBCwUAA4IBAQBeGZyhAOQ0HsTuVIF9r+8E0whlig1N4AufFRGfIJdTu7lulMF6IZ79hDqR4Fe+66/fjeBwCx3M9ulnjOglUcJLTmn9Fp1X/GwDs8HTP0h/uVByFnweSkbF1oDqea+/lmOnULwMaCLG+ibzvd5igG9QRWoc3xQJE0XNajj2SdlKmN8+o3TxhOdLfiDo5BoqF+XffwNtVR/QsLjaCiyM9rJXfetFRwH7aB/Slk9ygICXCdPP/kQz5T9dE5Lzi0bVe2OpiYUD6ZC38W0MDkmvEJv0v5heFxOsvcSfjZP1j8asg4EASiiiWMoQ6UI0kLtd8MJtTJRdQoxhXEiQdaz97AFj</X509Certificate></X509Data></KeyInfo></Signature><ns9:tidspunkt>2017-05-10T13:59:29.925+02:00</ns9:tidspunkt><ns9:feiltype>KLIENT</ns9:feiltype><ns9:detaljer>Kan ikke dekryptere dokumentpakke - brukes feil nøkkel?</ns9:detaljer></ns9:feil></ns3:StandardBusinessDocument>";
	private String inputBodyFail = "<ns3:StandardBusinessDocument xmlns:ns3=\"http://www.unece.org/cefact/namespaces/StandardBusinessDocumentHeader\" xmlns:ns9=\"http://begrep.difi.no/sdp/schema_v10\"><ns3:StandardBusinessDocumentHeader><ns3:HeaderVersion>1.0</ns3:HeaderVersion><ns3:Sender><ns3:Identifier Authority=\"urn:oasis:names:tc:ebcore:partyid-type:iso6523:9908\">9908:984661185</ns3:Identifier></ns3:Sender><ns3:Receiver><ns3:Identifier Authority=\"urn:oasis:names:tc:ebcore:partyid-type:iso6523:9908\">9908:991825827</ns3:Identifier></ns3:Receiver><ns3:DocumentIdentification><ns3:Standard>urn:no:difi:sdp:1.0</ns3:Standard><ns3:TypeVersion>1.0</ns3:TypeVersion><ns3:InstanceIdentifier>e6e9b21b-3fb0-413e-adfa-eaecb6b3c246</ns3:InstanceIdentifier><ns3:Type>feil</ns3:Type><ns3:CreationDateAndTime>2014-09-15T16:11:14.138+02:00</ns3:CreationDateAndTime></ns3:DocumentIdentification><ns3:BusinessScope><ns3:Scope><ns3:Type>ConversationId</ns3:Type><ns3:InstanceIdentifier>a9bb4f6d-05be-491c-92da-a10cbe83e3a1</ns3:InstanceIdentifier><ns3:Identifier>urn:no:difi:sdp:1.0</ns3:Identifier></ns3:Scope></ns3:BusinessScope></ns3:StandardBusinessDocumentHeader><ns9:feil><Signature xmlns=\"http://www.w3.org/2000/09/xmldsig#\"><SignedInfo><CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/><SignatureMethod Algorithm=\"http://www.w3.org/2001/04/xmldsig-more#rsa-sha256\"/><Reference URI=\"\"><Transforms><Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"/></Transforms><DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\"/><DigestValue>pMp1N+7u44Saz5MnzrS5bMRLiZfo3GBbJHauM4uSVS8=</DigestValue></Reference></SignedInfo><SignatureValue>B2VapuXkEpN9rKM7202ECoSpPVtaEGFK46hKTmNbrOCG3VS/jVvtpov5NB3sA2TWe6aeKWm0xhKdF207pDNrTYiYo/g5znQ4zQarRPXISN9d3sMCdJbci6R98G+uYFcFcZsMGE2WXENq2dVah0zLfa/UCCYxlZzfqQgRO5X79Pl1v145ygj0ipl68AhO6VWCAG4QzRKFJ/RDZZEBK4ZF/C5E6pKIgjGFnpgKPmRvI+6UuC0j93fxdjpJ179Kui87Fl8NUWAtOB7hPrOzDAchJubKxPWYDJNbo5XpOI0HbqxCu/eUHArLDhdqT4wvZk/eNxigOGGhurevzzomNgukAA==</SignatureValue><KeyInfo><X509Data><X509Certificate>MIIE7jCCA9agAwIBAgIKGBj1bv99Jpi+EzANBgkqhkiG9w0BAQsFADBRMQswCQYDVQQGEwJOTzEdMBsGA1UECgwUQnV5cGFzcyBBUy05ODMxNjMzMjcxIzAhBgNVBAMMGkJ1eXBhc3MgQ2xhc3MgMyBUZXN0NCBDQSAzMB4XDTE0MDQyNDEyMzExMVoXDTE3MDQyNDIxNTkwMFowVTELMAkGA1UEBhMCTk8xGDAWBgNVBAoMD1BPU1RFTiBOT1JHRSBBUzEYMBYGA1UEAwwPUE9TVEVOIE5PUkdFIEFTMRIwEAYDVQQFEwk5ODQ2NjExODUwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDLTnQryf2bmiyQ9q3ylQ6xMl7EhGIbjuziXkRTfL+M94m3ceAiko+r2piefKCiquLMK4j+UDcOapUtLC4dT4c6GhRH4FIOEn5aNS2I/njTenBypWka/VEhQUj7zvIh5G4UXIDIXYvLd7gideeMtkX24KUh2XVlh+PcqLGHirqBwVfFiTn5SKhr/ojhYYEb2xxTk3AY9nLd1MMffKQwUWmfoTos4scREYGI2R2vWxKWPcDqk+jig2DISWSJSuerz3HMYAAmp+Gjt0oFJNiyOFaFyGwT3DvqwOMQWwWXdmLh1NxMgTpghXAaXae76ucm9GDQ9E7ytf+JA096RWoi+5GtAgMBAAGjggHCMIIBvjAJBgNVHRMEAjAAMB8GA1UdIwQYMBaAFD+u9XgLkqNwIDVfWvr3JKBSAfBBMB0GA1UdDgQWBBTVyVLqcjWf1Qd0gsmCTrhXiWeqVDAOBgNVHQ8BAf8EBAMCBLAwFgYDVR0gBA8wDTALBglghEIBGgEAAwIwgbsGA1UdHwSBszCBsDA3oDWgM4YxaHR0cDovL2NybC50ZXN0NC5idXlwYXNzLm5vL2NybC9CUENsYXNzM1Q0Q0EzLmNybDB1oHOgcYZvbGRhcDovL2xkYXAudGVzdDQuYnV5cGFzcy5uby9kYz1CdXlwYXNzLGRjPU5PLENOPUJ1eXBhc3MlMjBDbGFzcyUyMDMlMjBUZXN0NCUyMENBJTIwMz9jZXJ0aWZpY2F0ZVJldm9jYXRpb25MaXN0MIGKBggrBgEFBQcBAQR+MHwwOwYIKwYBBQUHMAGGL2h0dHA6Ly9vY3NwLnRlc3Q0LmJ1eXBhc3Mubm8vb2NzcC9CUENsYXNzM1Q0Q0EzMD0GCCsGAQUFBzAChjFodHRwOi8vY3J0LnRlc3Q0LmJ1eXBhc3Mubm8vY3J0L0JQQ2xhc3MzVDRDQTMuY2VyMA0GCSqGSIb3DQEBCwUAA4IBAQCmMpAGaNplOgx3b4Qq6FLEcpnMOnPlSWBC7pQEDWx6OtNUHDm56fBoyVQYKR6LuGfalnnOKuB/sGSmO3eYlh7uDK9WA7bsNU/W8ZiwYwF6PBRui2rrqYk3kj4NLTNlyh/AOO1a2FDFHu369W0zcjj5ns7qs0K3peXtLX8pVxA8RmjwdGe69P/2r6s2A5CBj7oXZJD0Yo2dJFdsZzonT900sUi+MWzlhj3LxU5/684NWc2NI6ZPof/dyYpy3K/AFzpDLWGSmaDO66hPl7EfoJxEiX0DNBaQzNIyRFPh0ir0jM+32ZQ4goR8bAtyhKeTyA/4+Qx1WQXS3wURCC0lsbMh</X509Certificate></X509Data></KeyInfo></Signature><ns9:tidspunkt>2014-09-15T16:11:14.138+02:00</ns9:tidspunkt><ns9:feiltype>KLIENT</ns9:feiltype><ns9:detaljer>The sender '991825827 (part-id: adwaw)' is not registered in NAV Sparebank</ns9:detaljer></ns9:feil></ns3:StandardBusinessDocument>";
	@Rule
    public ExpectedException thrown = ExpectedException.none();

    @EndpointInject(uri = "direct:start")
    ProducerTemplate producer;
    @EndpointInject(uri = "mock:result")
    MockEndpoint result;

    @Test
    public void testKvitteringValidator() throws InterruptedException {
    	
    	result.expectedMessageCount(1);
        producer.sendBodyAndHeader(inputBody, KanalConstants.CAMEL_HEADER_LEGAL_ARCHIVE_LOG_ID, "DummylogID");
        result.assertIsSatisfied();
        Exchange ex = result.assertExchangeReceived(0);
        Assert.assertNotNull(ex.getIn().getBody());
    }
    
    @Test
    public void testKvitteringValidatorNegative() throws InterruptedException {
    	
    	result.expectedMessageCount(1);
    	try {
    		producer.sendBodyAndHeader(inputBodyFail, KanalConstants.CAMEL_HEADER_LEGAL_ARCHIVE_LOG_ID, "DummylogID");
    		Assert.fail("Exceptionb should be thrown");
		} catch (Exception e) {
			Assert.assertEquals("Cause should be right" , "Could not verfy signature cerificate", e.getCause().getMessage());
		}
    }
	
}
