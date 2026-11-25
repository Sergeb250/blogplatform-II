package com.amali.blogPlatform.service;

import com.amali.blogPlatform.dto.request.CreatePostRequest;
import com.amali.blogPlatform.dto.request.UpdatePostRequest;
import com.amali.blogPlatform.dto.response.PageResponse;
import com.amali.blogPlatform.dto.response.PostResponse;
import com.amali.blogPlatform.exception.DuplicateResourceException;
import com.amali.blogPlatform.exception.InvalidParameterException;
import com.amali.blogPlatform.exception.PostNotFoundException;
import com.amali.blogPlatform.exception.TagNotFoundException;
import com.amali.blogPlatform.exception.UserNotFoundException;
import com.amali.blogPlatform.model.Post;
import com.amali.blogPlatform.model.PostFilter;
import com.amali.blogPlatform.model.PostSortField;
import com.amali.blogPlatform.model.PostStatus;
import com.amali.blogPlatform.repository.CommentRepository;
import com.amali.blogPlatform.repository.PostRepository;
import com.amali.blogPlatform.repository.ReviewRepository;
import com.amali.blogPlatform.repository.TagRepository;
import com.amali.blogPlatform.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Business logic for posts. Shared by the REST controller and (Phase 7) the GraphQL controller. */
@Service
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final CommentRepository commentRepository;
    private final ReviewRepository reviewRepository;

    public PostService(PostRepository postRepository, UserRepository userRepository, TagRepository tagRepository,
                       CommentRepository commentRepository, ReviewRepository reviewRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.tagRepository = tagRepository;
        this.commentRepository = commentRepository;
        this.reviewRepository = reviewRepository;
    }

    /** The largest page a client may ask for, so nobody can request a million rows at once. */
    public static final int MAX_PAGE_SIZE = 100;

    public List<PostResponse> getAllPosts() {
        return postRepository.findAll().stream().map(PostResponse::from).toList();
    }

    /**
     * One page of posts that match the filter, sorted by {@code sortBy} in {@code direction}.
     * Pagination is done by the database (LIMIT / OFFSET), so only one page is ever loaded.
     *
     * @param page      page number, starting at 0
     * @param size      posts per page (1 to {@value #MAX_PAGE_SIZE})
     * @param sortBy    created_at (default), title or rating
     * @param direction ASC or DESC (default)
     */
    public PageResponse<PostResponse> getPosts(PostFilter filter, int page, int size, String sortBy, String direction) {
        checkPaging(page, size);
        PostSortField sortField = PostSortField.fromParameter(sortBy);    // 400 for an unknown column
        boolean ascending = PostSortField.isAscending(direction);        // 400 for anything but ASC/DESC

        int offset = page * size;                                         // page 2, size 10 → skip 20 rows
        List<PostResponse> content = postRepository.findPage(filter, sortField, ascending, size, offset)
                .stream().map(PostResponse::from).toList();
        long total = postRepository.countPosts(filter);
        return PageResponse.of(content, page, size, total);
    }

    /** Keyword search in title and content, paged and sorted like {@link #getPosts}. */
    public PageResponse<PostResponse> searchPosts(String keyword, int page, int size, String sortBy, String direction) {
        if (keyword == null || keyword.isBlank()) {
            throw new InvalidParameterException("keyword must not be empty");
        }
        return getPosts(new PostFilter(null, null, null, keyword), page, size, sortBy, direction);
    }

    private static void checkPaging(int page, int size) {
        if (page < 0) {
            throw new InvalidParameterException("page must be 0 or more (was " + page + ")");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidParameterException("size must be between 1 and " + MAX_PAGE_SIZE + " (was " + size + ")");
        }
    }

    public PostResponse getPostById(long id) {
        return PostResponse.from(findPost(id));
    }

    /**
     * Creates a post for an existing author, with optional tags. Inserting the post and its tag
     * links is one transaction: if a tag link fails, the post is not saved either.
     */
    @Transactional
    public PostResponse createPost(CreatePostRequest request) {
        if (!userRepository.existsById(request.authorId())) {
            throw new UserNotFoundException(request.authorId());
        }
        checkTitleIsFree(request.title(), 0);
        checkTagsExist(request.tagIds());

        PostStatus status = request.status() != null ? request.status() : PostStatus.DRAFT;
        Post newPost = new Post(null, request.authorId(), null, request.title(), request.content(),
                status, null, null, 0, null, null);
        long id = postRepository.insert(newPost);
        postRepository.replaceTags(id, request.tagIds());
        return getPostById(id);
    }

    /** Updates title, content, status and (when sent) tags. The author of a post stays the same. */
    @Transactional
    public PostResponse updatePost(long id, UpdatePostRequest request) {
        Post existing = findPost(id);
        // Only check when the title really changes: posts created before this rule existed may share a
        // title, and editing just their content must still work.
        if (!existing.title().trim().equalsIgnoreCase(request.title().trim())) {
            checkTitleIsFree(request.title(), id);
        }

        PostStatus status = request.status() != null ? request.status() : existing.status();
        Post changed = new Post(id, existing.authorId(), null, request.title(), request.content(),
                status, null, null, 0, null, null);
        postRepository.update(changed);

        if (request.tagIds() != null) {          // null = keep the old tags
            checkTagsExist(request.tagIds());
            postRepository.replaceTags(id, request.tagIds());
        }
        return getPostById(id);
    }

    /**
     * Soft-deletes a post (the row stays, with a {@code deleted_at} date) together with its comments and
     * reviews, in ONE transaction. Its tag links stay in post_tags; they are never shown for a deleted post.
     */
    @Transactional
    public void deletePost(long id) {
        if (postRepository.softDeleteById(id) == 0) {
            throw new PostNotFoundException(id);
        }
        commentRepository.softDeleteByPostId(id);
        reviewRepository.softDeleteByPostId(id);
    }

    private Post findPost(long id) {
        return postRepository.findById(id).orElseThrow(() -> new PostNotFoundException(id));
    }

    /**
     * Two posts may never have the same title (ignoring upper/lower case), whoever wrote them.
     * {@code postIdToIgnore} is the post being edited (0 for a new post), so it may keep its own title.
     * For new posts {@code @UniquePostTitle} already checked this; checking again here also covers
     * updates and GraphQL, and keeps the rule safe if the annotation is ever removed.
     */
    private void checkTitleIsFree(String title, long postIdToIgnore) {
        if (postRepository.existsByTitle(title, postIdToIgnore)) {
            throw new DuplicateResourceException("title", "A post titled '" + title + "' already exists");
        }
    }

    private void checkTagsExist(List<Long> tagIds) {
        if (tagIds == null) {
            return;
        }
        for (Long tagId : tagIds) {
            if (!tagRepository.existsById(tagId)) {
                throw new TagNotFoundException(tagId);
            }
        }
    }
}
