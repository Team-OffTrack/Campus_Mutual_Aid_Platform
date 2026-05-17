package cn.seecoder.campushelp.dto;

public class LoginResponse {

    private String token;
    private Long userId;
    private String name;
    private String role;
    private String avatar;

    public LoginResponse(String token, Long userId, String name, String role, String avatar) {
        this.token = token;
        this.userId = userId;
        this.name = name;
        this.role = role;
        this.avatar = avatar;
    }

    public String getToken() { return token; }
    public Long getUserId() { return userId; }
    public String getName() { return name; }
    public String getRole() { return role; }
    public String getAvatar() { return avatar; }
}
