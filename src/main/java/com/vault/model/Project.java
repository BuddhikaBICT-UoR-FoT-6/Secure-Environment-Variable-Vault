package com.vault.model;

// Project — represents one row in the "projects" SQLite table
// A Project is a named workspace that groups together a set of environment variable secrets (VaultEntry objects).

public class Project {
    // value is -1 if this object hasn't been saved to the DB yet
    private int id;

    private String name;
    private String launchCommand;

    // absolute path to the folder where the launch command should be executed
    private String workingDirectory;

    private String createdAt;

    // Full constructor — used by ProjectRepository when reading rows from the DB
    public Project(int id, String name, String launchCommand, String workingDirectory, String createdAt){
        this.id = id;
        this.name = name;
        this.launchCommand = launchCommand;
        this.workingDirectory = workingDirectory;
        this.createdAt = createdAt;
    }

    //  Creation constructor — used when the user creates a new project in the UI
    public Project(String name){
        this.id = -1; // not yet persisted to database
        this.name = name;
        this.launchCommand = null;
        this.workingDirectory = null;
        this.createdAt = null; // will be set by the database default value

    }

    // getters and setters
    // getters are used by ProjectRepository to read values when saving to the DB
    // setters are used by the UI when the user edits the project details
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLaunchCommand(){
        return launchCommand;
    }

    public void setLaunchCommand(String launchCommand) {
        this.launchCommand = launchCommand;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    // toString method is used when we want to display the project in the UI, such as in a dropdown list
    // by returning name here, the project list in the UI automatically
    @Override
    public String toString(){
        return name;
    }


}

