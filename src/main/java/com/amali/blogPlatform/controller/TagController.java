package com.amali.blogPlatform.controller;

import com.amali.blogPlatform.dto.request.TagRequest;
import com.amali.blogPlatform.dto.response.TagResponse;
import com.amali.blogPlatform.response.ApiResponse;
import com.amali.blogPlatform.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/** REST endpoints for tags. */
@RestController
@RequestMapping("/api/tags")
@Tag(name = "Tags")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @Operation(summary = "Create a tag")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Tag created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Missing or too long name"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "A tag with this name already exists")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<TagResponse>> createTag(@Valid @RequestBody TagRequest request) {
        TagResponse created = tagService.createTag(request);
        return ResponseEntity.created(URI.create("/api/tags/" + created.id()))
                .body(ApiResponse.created("Tag created successfully", created));
    }

    @Operation(summary = "List all tags", description = "Sorted by name.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "All tags")
    @GetMapping
    public ApiResponse<List<TagResponse>> getAllTags() {
        return ApiResponse.ok("Tags retrieved successfully", tagService.getAllTags());
    }

    @Operation(summary = "Get one tag")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "The tag"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Tag not found")
    })
    @GetMapping("/{id}")
    public ApiResponse<TagResponse> getTag(@PathVariable long id) {
        return ApiResponse.ok("Tag retrieved successfully", tagService.getTagById(id));
    }

    @Operation(summary = "Rename a tag")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tag updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid name"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Tag not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Another tag has this name")
    })
    @PutMapping("/{id}")
    public ApiResponse<TagResponse> updateTag(@PathVariable long id, @Valid @RequestBody TagRequest request) {
        return ApiResponse.ok("Tag updated successfully", tagService.updateTag(id, request));
    }

    @Operation(summary = "Delete a tag", description = "The tag is also removed from every post.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Tag deleted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Tag not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTag(@PathVariable long id) {
        tagService.deleteTag(id);
        return ResponseEntity.noContent().build();
    }
}
