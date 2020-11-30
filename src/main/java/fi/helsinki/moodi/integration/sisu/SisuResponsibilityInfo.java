package fi.helsinki.moodi.integration.sisu;

public class SisuResponsibilityInfo {
    public String roleUrn;
    // We have a PublicPerson here, so employeeId or EPPN are not available
    public String personId;

    public SisuResponsibilityInfo() {}

    public SisuResponsibilityInfo(String roleUrn, String teacherPersonId) {
        this.roleUrn = roleUrn;
        this.personId = teacherPersonId;
    }

}
