package no.nav.kanal.log;

public enum LogEvent {
	
	MELDING_HENTET_FRA_KO (125, "Melding hentet fra kø"),
	MELDING_POPULERT_MED_DOKUMENTER (162, "Melding populert med dokumenter"),
	ASICE_PRODUSERT (150, "Meldingens dokumentpakke (asice) produsert"),
	ASICE_KRYPTERT (101, "Meldingens dokumentpakke (asice) er kryptert til "),
	SBD_SIGNERT (150, "SBD er signert"),
	MELDING_SENDT_OK_TIL_DIFI (152,"Melding sendt OK til difi med leveringsvittering"),
	MELDING_SENDT_TIL_DIFI_FEILET (241,"Sending av melding til difi feilet"),
	MELDING_SENDT_TIL_DIFI_FEILET_UKJENT_TRANSPORTKVITTERING (207, "Transportkvittering fra difi er ikke motatt"),
	
	KVITTERING_MOTTATT_FRA_DIFI(119, "Kvittering mottatt fra difi"),
	KVITTERING_SIGNATUR_VALIDERT_OK(133,"Kvitteringssignatur validert OK"),
	KVITTERING_LAGT_PA_KO (187, "Kvittering lagt på kø"),
	MELDING_SKAL_SENDES_TIL_BOQ(218,"Melding sendes til BOQ. Error: ");
	
	
	/*
	 * PK-14484:
	 * 	X		Melding hentet fra kø
	 * 	X		Melding lagt på kø (kvittering/feil), også backout kø
	 * 	X		Melding populert med dokumenter
	 * 
	 * PK-14486:
	 * 	X		Melding ASIC generert (logg med ASIC og SBD)
	 * 	X		Melding kryptert (med metadata om kryptooperasjon, utsteder, serial, ...)
	 * 	X		Melding signert
	 *  X		Kvittering validert
	 * 
	 * PK-14487:
	 * 	X		Vellykket forsendelse til Difi (transportkvittering logges)
	 * 	X		Feilet forsendelse til Difi
	 * 	X		Mottak av kvittering fra Difi
	 * 
	 */
	
	private int eventNo;
	private String description;

	private LogEvent(int eventNo, String description) {
		
		this.eventNo = eventNo;
		this.description = description;
	}

	public int getEventNo() {
		return eventNo;
	}


	public String getDescription() {
		return description;
	}
	
}
