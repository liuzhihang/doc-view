package com.liuzhihang.doc.view.integration.dto;

import lombok.Data;

/**
 * 返回对象参考 https://www.yuque.com/yuque/developer/doc
 *
 * @author liuzhihang
 * @date 2022/3/31 23:11
 */
@Data
public class YuQueResponse {


    private Data data;
    private Abilities abilities;

    @lombok.Data
    public static class Data {
        private Long wordCount;
        private Long viewStatus;
        private Long userId;
        private String updatedAt;
        private String title;
        private Long status;
        private String slug;
        private Long readStatus;
        private String publishedAt;
        private Long publicX;
        private Long likesCount;
        private Long id;
        private Long hits;
        private String format;
        private String firstPublishedAt;
        private String description;
        private String createdAt;
        private String contentUpdatedAt;
        private Long commentsCount;
        private Long bookId;
        private String bodyLake;
        private String bodyHtml;
        private String bodyDraftLake;
        private String bodyDraft;
        private String body;
        private String Serializer;
        private Creator creator;
        private Book book;

        @lombok.Data
        public static class Creator {
            private String updatedAt;
            private String type;
            private Long publicBooksCount;
            private String name;
            private String login;
            private Long id;
            private Long followingCount;
            private Long followersCount;
            private String description;
            private String createdAt;
            private Long booksCount;
            private String avatarUrl;
            private String Serializer;
        }

        @lombok.Data
        public static class Book {
            private Long watchesCount;
            private Long userId;
            private String updatedAt;
            private String type;
            private String slug;
            private Long publicX;
            private String namespace;
            private String name;
            private Long likesCount;
            private Long itemsCount;
            private Long id;
            private String description;
            private Long creatorId;
            private String createdAt;
            private String contentUpdatedAt;
            private String Serializer;
            private User user;

            @lombok.Data
            public static class User {
                private String updatedAt;
                private String type;
                private Long publicBooksCount;
                private String name;
                private String login;
                private Long id;
                private Long followingCount;
                private Long followersCount;
                private String description;
                private String createdAt;
                private Long booksCount;
                private String avatarUrl;
                private String Serializer;
            }
        }
    }

    @lombok.Data
    public static class Abilities {
        private Boolean update;
        private Boolean destroy;
    }
}
