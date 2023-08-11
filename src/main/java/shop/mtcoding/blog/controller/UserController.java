package shop.mtcoding.blog.controller;

import java.io.BufferedReader;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import shop.mtcoding.blog.dto.JoinDTO;
import shop.mtcoding.blog.dto.LoginDTO;
import shop.mtcoding.blog.dto.UserUpdateDTO;
import shop.mtcoding.blog.model.User;
import shop.mtcoding.blog.repository.UserRepository;

@Controller
// view 잘 받고, 잘 return하기
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HttpSession session;
    // request는 가방, session 서랍
    // 로그인이 되면 session에 저장해야 한다.

    @GetMapping("/joinForm")
    public String joinForm() {

        return "user/joinForm";
    }

    @GetMapping("/loginForm")
    public String loginForm() {

        return "user/loginForm";
    }

    // 회원 정보 수정 페이지 - select
    @GetMapping("/user/updateForm")
    public String updateForm(HttpServletRequest request) {
        // 부가로직
        // 1. 로그인 인증
        User sesseionUser = (User) session.getAttribute("sessionUser");
        if (sesseionUser == null) {
            return "redirect:/loginForm";
        }

        // ★★ 핵심로직
        User user = userRepository.findByUsername(sesseionUser.getUsername());
        request.setAttribute("user", user);

        return "user/updateForm";
    }

    // 회원 정보 수정 기능
    @PostMapping("/user/update")
    public String update(UserUpdateDTO userUpdateDTO) {
        // 부가로직
        // 1. 로그인 인증
        User sessionUser = (User) session.getAttribute("sessionUser");
        if (sessionUser == null) {
            return "redirect:/loginForm";
        }

        // 핵심로직
        User user = userRepository.findByUsername(sessionUser.getUsername());
        userRepository.update(userUpdateDTO, sessionUser.getId());
        return "redirect:/";
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////

    // 4. 실무 -> 이 방법을 사용해야 한다.
    // @PostMapping("/join")
    public String join(JoinDTO joinDTO) {
        // 부가로직
        // 유효성검사 = validation check
        // 공백, null 체크(우회 접근 제한)
        if (joinDTO.getUsername() == null || joinDTO.getUsername().isEmpty()) {
            return "redirct:/40x";
        }
        if (joinDTO.getPassword() == null || joinDTO.getPassword().isEmpty()) {
            return "redirct:/40x";
        }
        if (joinDTO.getEmail() == null || joinDTO.getEmail().isEmpty()) {
            return "redirct:/40x";
        }

        // 핵심로직
        // 중복 방지
        try {
            userRepository.save(joinDTO);

        } catch (Exception e) {
            return "redirect:/50x";
        }

        return "redirect:/loginForm";
    }

    // 안전한 코드 작성 실습(오류가 터질 것은 try-catch가 아닌 코드로 터지지 않게 막는다.)
    @PostMapping("/join")
    public String join1(JoinDTO joinDTO) {

        // 부가로직
        if (joinDTO.getUsername() == null || joinDTO.getUsername().isEmpty()) {
            return "redirct:/40x";
        }
        if (joinDTO.getPassword() == null || joinDTO.getPassword().isEmpty()) {
            return "redirct:/40x";
        }
        if (joinDTO.getEmail() == null || joinDTO.getEmail().isEmpty()) {
            return "redirct:/40x";
        }

        // DB에 해당 username이 있는 지 체크해보기
        // -> JPA(기본 메소드 활용, 기술에 대한 원리 공부)
        User user = userRepository.findByUsername(joinDTO.getUsername());
        // -> 에러에 대한 처리를 담당하는 클래스 하나 생성 => 위임 => 자바스크립트로 변경
        if (user != null) {
            return "redirect:/50x";
        }

        // 핵심기능
        userRepository.save(joinDTO);
        return "redirect:/loginForm";

    }

    // localhost:8080/check?username=ssar
    // 스프링과 JS 연결하기 - 중복체크 버튼 = AJAX 통신
    @GetMapping("/check")
    public ResponseEntity<String> check(String username) {

        User user = userRepository.findByUsername(username); // null오류는 try-catch는 repository에
        if (user != null) {
            return new ResponseEntity<String>("유저네임이 중복되었습니다.", HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<String>("유저네임을 사용할 수 있습니다.", HttpStatus.OK);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////
    @PostMapping("/login")
    public String login(LoginDTO loginDTO) {
        // 부가로직
        // 유효성검사 = validation check
        // 공백, null 체크(우회 접근 제한)
        if (loginDTO.getUsername() == null || loginDTO.getUsername().isEmpty()) {
            return "redirect:/40x";
        }
        if (loginDTO.getPassword() == null || loginDTO.getPassword().isEmpty()) {
            return "redirect:/40x";
        }

        // 회원가입된 리스트에서 login유저의 username찾기
        User user = userRepository.findByUsername(loginDTO.getUsername());
        System.out.println("logintest : " + user.getPassword());
        System.out.println("logintest : " + loginDTO.getPassword());

        // login유저의 비밀번호를 해시코드로 변환하기
        String encPassword = BCrypt.hashpw(loginDTO.getPassword(), BCrypt.gensalt());
        System.out.println("logintest : - encPassword  : " + encPassword);

        // login유저가 입력한 해시코드와 회원가입할 때 저장된 해시코드를 true/false로 비교하기
        boolean isValid = BCrypt.checkpw(loginDTO.getPassword(), encPassword);
        System.out.println("logintest : " + isValid);

        // true이면 아래의 코드가 진행되도록 하기
        try {
            if (isValid == true) {
                session.setAttribute("sessionUser", user);
                return "redirect:/";
            } else {
                return "redirect:/LoginForm"; // UX-자바스크립트하기
            }
        } catch (Exception e) {
            return "redirect:/exLogin";
        }
    }

    @GetMapping("/logout")
    public String logout() {
        session.invalidate();
        // 내 sesson과 관련된 것 다 지우기
        // 세션 무효화
        return "redirect:/";
    }

}
