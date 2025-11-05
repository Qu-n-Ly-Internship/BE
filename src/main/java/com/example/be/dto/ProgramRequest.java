package com.example.be.dto;

import java.util.Date;

public class ProgramRequest {
    private final Long id;
    private final String programName;
    private final String description;
    private final Long hrId;
    private final String hrName;
    private final Date dateCreate;
    private final Date dateEnd;

    public ProgramRequest(Long id, String programName, String description,
                          Long hrId, String hrName,
                          Date dateCreate, Date dateEnd) {
        this.id = id;
        this.programName = programName;
        this.description = description;
        this.hrId = hrId;
        this.hrName = hrName;
        this.dateCreate = dateCreate;
        this.dateEnd = dateEnd;
    }

    // --- Getters ---
    public Long getId() {
        return id;
    }

    public String getProgramName() {
        return programName;
    }

    public String getDescription() {
        return description;
    }

    public Long getHrId() {
        return hrId;
    }

    public String getHrName() {
        return hrName;
    }

    public Date getDateCreate() {
        return dateCreate;
    }

    public Date getDateEnd() {
        return dateEnd;
    }
}
