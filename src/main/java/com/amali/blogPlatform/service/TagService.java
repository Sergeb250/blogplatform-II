package com.amali.blogPlatform.service;

import com.amali.blogPlatform.dto.request.TagRequest;
import com.amali.blogPlatform.dto.response.TagResponse;
import com.amali.blogPlatform.exception.DuplicateResourceException;
import com.amali.blogPlatform.exception.TagNotFoundException;
import com.amali.blogPlatform.model.Tag;
import com.amali.blogPlatform.repository.TagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** Business logic for tags. Tag names are unique. */
@Service
@Transactional(readOnly = true)
public class TagService {

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    public List<TagResponse> getAllTags() {
        return tagRepository.findAll().stream().map(TagResponse::from).toList();
    }

    public TagResponse getTagById(long id) {
        return tagRepository.findById(id)
                .map(TagResponse::from)
                .orElseThrow(() -> new TagNotFoundException(id));
    }

    /**
     * Creates a tag. If a tag with this name was deleted before, that tag is brought back instead
     * (same id), because tag names are UNIQUE and the deleted row still holds the name.
     */
    @Transactional
    public TagResponse createTag(TagRequest request) {
        Optional<Long> restoredId = tagRepository.restoreDeletedByName(request.name());
        if (restoredId.isPresent()) {
            return getTagById(restoredId.get());
        }
        checkNameIsFree(request.name(), 0);
        long id = tagRepository.insert(new Tag(null, request.name()));
        return getTagById(id);
    }

    @Transactional
    public TagResponse updateTag(long id, TagRequest request) {
        if (!tagRepository.existsById(id)) {
            throw new TagNotFoundException(id);
        }
        checkNameIsFree(request.name(), id);
        tagRepository.update(new Tag(id, request.name()));
        return getTagById(id);
    }

    /**
     * Soft-deletes a tag: it stays in the table with a {@code deleted_at} date, and every query that
     * reads tags skips it, so it disappears from all posts and from the tag filter.
     */
    @Transactional
    public void deleteTag(long id) {
        if (tagRepository.softDeleteById(id) == 0) {
            throw new TagNotFoundException(id);
        }
    }

    /**
     * {@code tagIdToIgnore} is the tag being renamed (0 for a new tag), so keeping its own name is allowed.
     * Deleted tags count too: their row still holds the UNIQUE name.
     */
    private void checkNameIsFree(String name, long tagIdToIgnore) {
        if (tagRepository.isNameTakenByAnother(name, tagIdToIgnore)) {
            throw new DuplicateResourceException("name", "Tag '" + name + "' already exists");
        }
    }
}
