package app.dto;

public class AssignmentTypeDTO {

    private Integer id;
    private String name;
    private Boolean isActive;

    public AssignmentTypeDTO() {}

    public AssignmentTypeDTO(Integer id, String name, Boolean isActive) {
        this.id = id;
        this.name = name;
        this.isActive = isActive;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean active) { isActive = active; }
}