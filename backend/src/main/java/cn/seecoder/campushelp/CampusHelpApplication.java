package cn.seecoder.campushelp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("cn.seecoder.campushelp.mapper")
@EnableScheduling
public class CampusHelpApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusHelpApplication.class, args);
    }
}
