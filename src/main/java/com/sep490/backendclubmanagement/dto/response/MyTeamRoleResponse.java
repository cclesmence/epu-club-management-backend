    package com.sep490.backendclubmanagement.dto.response;

    import lombok.*;
    import java.util.List;

    @Getter @Setter @Builder
    @AllArgsConstructor @NoArgsConstructor
    public class MyTeamRoleResponse {
        private Long teamId;
        private String teamName;
        private String description;

        private boolean member;
        private List<String> myRoles;
    
        private int memberCount;

        private List<MemberBrief> members;

        @Getter @Setter @Builder
        @AllArgsConstructor @NoArgsConstructor
        public static class MemberBrief {
            private Long userId;
            private String fullName;
            private String avatarUrl;
            private String roleName;
            private String email;
            private String studentCode;
        }
    }
