package org.openmrs.module.emrapi.visit;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Visit;
import org.openmrs.module.emrapi.EmrApiProperties;
import org.openmrs.module.emrapi.diagnosis.Diagnosis;
import org.openmrs.module.emrapi.diagnosis.DiagnosisMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

// TODO: merge this with VisitSummary
public class VisitDomainWrapper {

    private static final Log log = LogFactory.getLog(VisitDomainWrapper.class);

    @Autowired
    @Qualifier("emrApiProperties")
    EmrApiProperties emrApiProperties;

    private Visit visit;

    public VisitDomainWrapper(Visit visit) {
        this.visit = visit;
    }

    public Visit getVisit() {
        return visit;
    }

    public void setEmrApiProperties(EmrApiProperties emrApiProperties) {
        this.emrApiProperties = emrApiProperties;
    }

    public int getDifferenceInDaysBetweenCurrentDateAndStartDate() {
        Date today = Calendar.getInstance().getTime();

        Calendar startDateVisit = getStartDateVisit();

        int millisecondsInADay = 1000 * 60 * 60 * 24;

        return (int) ((today.getTime() - startDateVisit.getTimeInMillis()) / millisecondsInADay);
    }

    public List<Diagnosis> getPrimaryDiagnoses() {
        List<Diagnosis> diagnoses = new ArrayList<Diagnosis>();
        DiagnosisMetadata diagnosisMetadata = emrApiProperties.getDiagnosisMetadata();
        for (Encounter encounter : visit.getEncounters()) {
            if (!encounter.isVoided()) {
                for (Obs obs : encounter.getObsAtTopLevel(false)) {
                    if (diagnosisMetadata.isDiagnosis(obs)) {
                        try {
                            Diagnosis diagnosis = diagnosisMetadata.toDiagnosis(obs);
                            if (Diagnosis.Order.PRIMARY == diagnosis.getOrder()) {
                                diagnoses.add(diagnosis);
                            }
                        } catch (Exception ex) {
                            log.warn("malformed diagnosis obs group with obsId " + obs.getObsId(), ex);
                        }
                    }
                }
            }
        }
        return diagnoses;
    }

    private Calendar getStartDateVisit() {
        Date startDatetime = visit.getStartDatetime();
        Calendar startDateCalendar = Calendar.getInstance();
        startDateCalendar.setTime(startDatetime);
        startDateCalendar.set(Calendar.HOUR_OF_DAY, 0);
        startDateCalendar.set(Calendar.MINUTE, 0);
        startDateCalendar.set(Calendar.SECOND, 0);
        return startDateCalendar;
    }
}
