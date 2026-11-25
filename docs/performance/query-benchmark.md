| Query (100,000 posts) | Without indexes (ms) | With indexes (ms) | Faster |
|---|---:|---:|---:|
| GET /api/posts (page 0, newest first) | 286.39 | 2.67 | 107.2x |
| GET /api/posts?status=PUBLISHED | 267.31 | 2.38 | 112.1x |
| GET /api/posts?authorId=7 | 8.66 | 2.33 | 3.7x |
| GET /api/posts?sortBy=title&direction=ASC | 291.30 | 2.16 | 134.6x |
| GET /api/posts?tag=tag-5 | 132.16 | 129.51 | 1.0x |
| GET /api/posts?sortBy=rating | 1671.39 | 1728.31 | 1.0x |
| GET /api/posts?page=5000 (deep page, OFFSET 50000) | 463.16 | 420.97 | 1.1x |
| GET /api/posts/search?keyword=topic 7 (LIKE, common words) | 491.71 | 7.27 | 67.6x |
| GET /api/posts/search?keyword=77777 (LIKE, rare word) | 427.37 | 661.24 | 0.6x |
| COUNT for totalElements (no filter) | 25.13 | 20.53 | 1.2x |
| Search for rare word 77777: FULLTEXT MATCH ... AGAINST | – | 0.94 | – |
