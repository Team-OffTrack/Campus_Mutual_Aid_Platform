package cn.seecoder.campushelp.dto;

import jakarta.validation.constraints.NotBlank;

public class LoginRequest {

    @NotBlank(message = "学号不能为空")
    private String studentId;

    @NotBlank(message = "密码不能为空")
    private String password;

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
