package com.example.be.dto;

import java.util.Date;

public class ProgramRequest {
    private String programName;
    private Date dateCreate;
    private Date dateEnd;
    private String description;
    private Long hrId; // 👈 cái này client gửi lên

    // getter + setter
    public String getProgramName() { return programName; }
    public void setProgramName(String programName) { this.programName = programName; }

    public Date getDateCreate() { return dateCreate; }
    public void setDateCreate(Date dateCreate) { this.dateCreate = dateCreate; }

    public Date getDateEnd() { return dateEnd; }
    public void setDateEnd(Date dateEnd) { this.dateEnd = dateEnd; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getHrId() { return hrId; }
    public void setHrId(Long hrId) { this.hrId = hrId; }
}
