package cwm.reasoning;

public class InputData {
  private String patient;
  private String rule;
  private String new_patient;
  
  public InputData(){ 
  }
  public InputData (String patient, String rule, String new_patient){
    this.patient = patient;
    this.rule = rule;
    this.new_patient = new_patient;
  }
  
  public String getPatient() {
    return patient;
  }
  
  public void setPatient(String patient) {
    this.patient = patient;
  }
  
  public String getRule() {
	return rule;
  }
  
  public void setRule(String rule) {
	this.rule = rule;
  }
	  
  public String getNew_patient() {
	return new_patient;
  }
  
  public void setNew_patient(String new_patient) {
	this.new_patient = new_patient;
  }

}
