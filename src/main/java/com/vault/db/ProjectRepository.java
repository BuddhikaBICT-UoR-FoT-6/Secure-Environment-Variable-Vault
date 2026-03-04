package com.vault.db;

import com.vault.model.Project;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

// all SQL operations for the "projects" table
// preparedStatements are the standard defence against SQL Injection attacks
// by ensuring user input is treated as data, not executable code


public class ProjectRepository {
    // JDBC connection obtained from DatabaseManager
    private final Connection conn;

    public ProjectRepository(Connection conn){
        this.conn = conn;
    }

    // inserts a new project row into the database
    // @param project a Project with name set; id should be -1 before calling this
    // @return the same project object, now with its real database id set
    public Project createProject(Project project){
        // ? = placeholder for a parameter
        // id is auto incremented
        String sql = "INSERT INTO projects (name, launch_cmd, working_dir) VALUES (?, ?, ?)";

        try(
                // RETURN_GENERATED_KEYS -> after insert, give back the auto-generated primary key
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
                ){
            // set the parameters in the SQL statement -> setString(parameterIndex, value)
            // each call fills in one "?" placeholder in order
            stmt.setString(1, project.getName());
            stmt.setString(2, project.getLaunchCommand());
            stmt.setString(3, project.getWorkingDirectory());

            // executeUpdate() runs INSERT, UPDATE, or DELETE statements
            // returns the number of rows affected
            stmt.executeUpdate();

            // getGeneratedKeys() returns a ResultSet with the auto-generated id
            try(ResultSet keys = stmt.getGeneratedKeys()){
                if(keys.next()){
                    // getLong(1) = first column of the first row = the new id
                    int generatedId = (int) keys.getLong(1);

                    // update the Java object with the real DB id
                    project.setId(generatedId);
                }

            }

            System.out.println("[ProjectRepo] Created project: " + project.getName()
                    + " (id=" + project.getId() + ")");
            return project;

        } catch (SQLException e){
            throw new RuntimeException("Failed to create project: " + project.getName(), e);
        }
    }

    // retrieves all projects from the database, ordered by name alphabetically
    // @return a List of Project objects
    public List<Project> listProjects(){
        String sql = "SELECT id, name, launch_cmd, working_dir, created_at FROM projects ORDER BY name ASC";

        // accumulate results into this list
        List<Project> projects = new ArrayList<>();

        try(
                PreparedStatement stmt = conn.prepareStatement(sql);
                // resultSet is like a cursor over the rows returned by the query
                ResultSet rs = stmt.executeQuery() // executeQuery() runs SELECT statements and returns a ResultSet
                ){
            // advance through the ResultSet row by row
            while(rs.next()){
                Project p = new Project(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("launch_cmd"),
                        rs.getString("working_dir"),
                        rs.getString("created_at"));

                projects.add(p);
            }
        } catch(SQLException e){
            throw new RuntimeException("Failed to list projects", e);
        }
        return projects;
    }

    // updates an existing project's details in the database
    // @param project the Project with updated field values
    public void updateProject(Project project){
        String sql = "UPDATE projects SET name = ?, launch_cmd = ?, working_dir = ? WHERE id = ?";

        try(PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setString(1, project.getName());
            stmt.setString(2, project.getLaunchCommand());
            stmt.setString(3, project.getWorkingDirectory());
            stmt.setInt(4, project.getId());

            int rowsAffected = stmt.executeUpdate();

            if(rowsAffected == 0){
                System.err.println("[ProjectRepo] Warning: No project found with id " + project.getId() + " to update.");

            }

        } catch(SQLException e){
            throw new RuntimeException("Failed to update project: " + project.getId(), e);
        }
    }

    // deletes a project and ALL its vault entries from the database
    // cascade delete - the "vault_entries" table has a foreign key constraint with ON DELETE CASCADE
    // deleting the project row triggers SQLite to also delete all vault_entries
    public void deleteProject(int projectId){
        String sql = "DELETE FROM projects WHERE id = ?";

        try(PreparedStatement stmt  = conn.prepareStatement(sql)){
            stmt.setInt(1, projectId);

            int rowsAffected = stmt.executeUpdate();

            System.out.println("[ProjectRepo] Deleted project id " + projectId + ". Rows affected: " + rowsAffected);

        } catch(SQLException e){
            throw new RuntimeException("Failed to delete project: " + projectId, e);
        }

    }



}
