package com.mbancer.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.mbancer.domain.Project;
import com.mbancer.exceptions.NoSuchUserException;
import com.mbancer.service.ProjectService;
import com.mbancer.web.rest.dto.UserDTO;
import com.mbancer.web.rest.util.HeaderUtil;
import com.mbancer.web.rest.util.PaginationUtil;
import com.mbancer.web.rest.dto.ProjectDTO;
import com.mbancer.web.rest.mapper.ProjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing Project.
 */
@RestController
@RequestMapping("/api")
public class ProjectResource {

    private final Logger log = LoggerFactory.getLogger(ProjectResource.class);

    @Inject
    private ProjectService projectService;

    @Inject
    private ProjectMapper projectMapper;

    /**
     * POST  /projects : Create a new project.
     *
     * @param projectDTO the projectDTO to create
     * @return the ResponseEntity with status 201 (Created) and with body the new projectDTO, or with status 400 (Bad Request) if the project has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @RequestMapping(value = "/projects",
        method = RequestMethod.POST,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<ProjectDTO> createProject(@Valid @RequestBody ProjectDTO projectDTO) throws URISyntaxException {
        log.debug("REST request to save Project : {}", projectDTO);
        if (projectDTO.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("project", "idexists", "A new project cannot already have an ID")).body(null);
        }
        ProjectDTO result = projectService.save(projectDTO);
        return ResponseEntity.created(new URI("/api/projects/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert("project", result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /projects : Updates an existing project.
     *
     * @param projectDTO the projectDTO to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated projectDTO,
     * or with status 400 (Bad Request) if the projectDTO is not valid,
     * or with status 500 (Internal Server Error) if the projectDTO couldnt be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @RequestMapping(value = "/projects",
        method = RequestMethod.PUT,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<ProjectDTO> updateProject(@Valid @RequestBody ProjectDTO projectDTO) throws URISyntaxException {
        log.debug("REST request to update Project : {}", projectDTO);
        if (projectDTO.getId() == null) {
            return createProject(projectDTO);
        }
        ProjectDTO result = projectService.save(projectDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert("project", projectDTO.getId().toString()))
            .body(result);
    }

    /**
     * GET  /projects : get all the projects.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of projects in body
     * @throws URISyntaxException if there is an error to generate the pagination HTTP headers
     */
    @RequestMapping(value = "/projects",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<List<ProjectDTO>> getAllProjects(Pageable pageable)
        throws URISyntaxException {
        log.debug("REST request to get a page of Projects");
        Page<Project> page = projectService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/projects");
        return new ResponseEntity<>(projectMapper.projectsToProjectDTOs(page.getContent()), headers, HttpStatus.OK);
    }

    /**
     * GET  /projects/:id : get the "id" project.
     *
     * @param id the id of the projectDTO to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the projectDTO, or with status 404 (Not Found)
     */
    @RequestMapping(value = "/projects/{id}",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<ProjectDTO> getProject(@PathVariable Long id) {
        log.debug("REST request to get Project : {}", id);
        ProjectDTO projectDTO = projectService.findOne(id);
        return Optional.ofNullable(projectDTO)
            .map(result -> new ResponseEntity<>(
                result,
                HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * DELETE  /projects/:id : delete the "id" project.
     *
     * @param id the id of the projectDTO to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @RequestMapping(value = "/projects/{id}",
        method = RequestMethod.DELETE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        log.debug("REST request to delete Project : {}", id);
        projectService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert("project", id.toString())).build();
    }

    @RequestMapping(value = "/projects/{projectId}/addTask/{taskId}",
        method = RequestMethod.PUT,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> addTaskToProject(@PathVariable long projectId, @PathVariable long taskId){
        log.debug("REST request to add task : {} to project : {}", taskId, projectId);
        projectService.addTaskToProject(projectId, taskId);
        return ResponseEntity.ok().build();
    }

    @RequestMapping(value = "/projects/byUser/{userId}",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Page<ProjectDTO>> projectsByUser(@PathVariable("userId") Long userId, Pageable pageable){
        log.debug("REST request to get projects by user : {}", userId);
        return ResponseEntity.ok(projectService.getByUser(userId, pageable));
    }

    @RequestMapping(value = "/projects/byCurrentUser",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Page<ProjectDTO>> projectsByCurrentUser(Pageable pageable){
        log.debug("REST request to get projects by currently logged user");
        return ResponseEntity.ok(projectService.getByCurrentUser(pageable));
    }

    @RequestMapping(value = "/projects/{projectId}/members",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Page<UserDTO>> membersOfProject(@PathVariable("projectId") Long projectId, Pageable pageable){
        log.debug("REST request to get members of project : {}", projectId);
        return ResponseEntity.ok(projectService.getMembers(projectId, pageable));
    }

    @RequestMapping(value = "/projects/{projectId}/addMember/{email}",
        method = RequestMethod.PUT,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addMemberToProject(@PathVariable("projectId") Long projectId, @PathVariable("email") String email){
        log.debug("REST request to add member : {} to project : {}", email, projectId);
        try {
            projectService.addMemberToProject(projectId, email);
            return ResponseEntity.ok().build();
        } catch (NoSuchUserException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @RequestMapping(value = "/projects/{projectId}/deleteMember/{email}",
        method = RequestMethod.PUT,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteMemberFromProject(@PathVariable("projectId") Long projectId, @PathVariable("email") String email){
        log.debug("REST request to delete member : {} from project : {}", email, projectId);
        try {
            projectService.deleteMemberFromProject(projectId, email);
            return ResponseEntity.ok().build();
        } catch (NoSuchUserException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}
