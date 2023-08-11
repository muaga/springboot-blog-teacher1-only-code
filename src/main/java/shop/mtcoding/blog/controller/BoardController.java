package shop.mtcoding.blog.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import shop.mtcoding.blog.dto.BoardDetailDTO;
import shop.mtcoding.blog.dto.UpdateDTO;
import shop.mtcoding.blog.dto.WriteDTO;
import shop.mtcoding.blog.model.Board;
import shop.mtcoding.blog.model.Reply;
import shop.mtcoding.blog.model.User;
import shop.mtcoding.blog.repository.BoardRepository;
import shop.mtcoding.blog.repository.ReplyRepository;

@Controller
public class BoardController {

    // 로그인을 한 후, 글쓰기를 할 수 있도록 session 데이터 가지고 오기
    @Autowired
    private HttpSession session;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private ReplyRepository replyRepository;

    @Autowired
    private ErrorController errorController;

    @ResponseBody
    // 변수의 데이터를 JSON으로 보여준다.
    // 객체의 데이터를 보고 싶을 때 해당 코드를 사용해보면 된다.
    @GetMapping("/test/dtos")
    public List<BoardDetailDTO> test2() {
        List<BoardDetailDTO> dtos = boardRepository.findByIdJoinReply(1, null);
        for (BoardDetailDTO boardDetailDTO : dtos) {
            System.out.println("test : " + boardDetailDTO.getBoardTitle());
        }
        return dtos;
        // model-board에 user 객체를 가지고 있기 때문에, user 데이터도 자동으로 호출한다.
        // user 객체를 가져서 board가 FK임을 확인-연결이 되어 있기 때문이다.
    }

    @ResponseBody
    // 변수의 데이터를 JSON으로 보여준다.
    @GetMapping("/test/board/1")
    public Board test(Integer id) {
        Board board = boardRepository.findById(1);
        return board;
        // model-board에 user 객체를 가지고 있기 때문에, user 데이터도 자동으로 호출한다.
        // user 객체를 가져서 board가 FK임을 확인-연결이 되어 있기 때문이다.
    }

    // 게시물 수정 페이지
    @GetMapping("/board/{id}/updateForm")
    public String updateForm(@PathVariable Integer id, HttpServletRequest request) {
        // 부가로직
        // 1. 인증 검사
        User sessionUser = (User) session.getAttribute("sessionUser");
        if (sessionUser == null) {
            return "redirect:/loginForm"; // 401
        }

        // 2. 권한 체크
        Board board = boardRepository.findById(id);
        if (sessionUser.getId() != board.getUser().getId()) {
            return "redirect:/40x"; // 403 권한없음
        }

        // 핵심로직
        request.setAttribute("board", board);
        return "board/updateForm";
    }

    // 게시물 수정 기능
    @PostMapping("/board/{id}/update")
    public String update(@PathVariable Integer id, UpdateDTO updateDTO) {
        // 부가로직
        // 1. 인증 검사
        User sessionUser = (User) session.getAttribute("sessionUser");
        if (sessionUser == null) {
            return "redirect:/loginForm";
        }

        // 2. 권한 체크
        Board board = boardRepository.findById(id);
        if (sessionUser.getId() != board.getUser().getId()) {
            return "redirect:/40x";
        }

        // 핵심로직
        boardRepository.update(updateDTO, id);
        return "redirect:/board/" + id;
    }

    // 게시물 삭제 기능
    @PostMapping("/board/{id}/delete")
    public String delete(@PathVariable Integer id) { // PathVariable 값 받기
        // 부가로직
        // 1. 인증검사(우회 접근 제한)
        // session에 접근해서 sessionUser key 값을 가져 오세요
        // null이면, 로그인 페이지로 보내고
        // null이 아니면, 3번을 실행하세요.
        User sessionUser = (User) session.getAttribute("sessionUser");
        if (sessionUser == null) {
            return "redirect:/loginForm"; // 401
        }

        // 2.권한 체크(우회 접근 제한)
        Board board = boardRepository.findById(id);
        if (sessionUser.getId() != board.getUser().getId()) {
            return "redirect:/40x"; // 403 권한없음
        }

        // 3. model에 접근해서 삭제
        // boardRepository.deleteById(id); 호출하세요 -> 리턴을 받지 마세요
        // "delete from board_tb where id = :id"
        boardRepository.deleteById(id);

        return "redirect:/";
    }

    // 게시물목록 페이지
    // 유효성 검사, 인증 검사 X
    @GetMapping({ "/", "/board" })
    public String index(String keyword,
            @RequestParam(defaultValue = "0") Integer page,
            HttpServletRequest request) {
        // get요청에서 매개변수는 쿼리스트링으로 받을 수 있다.
        // @RequestParam은 쿼리스트링을 파싱해준다.
        // loaclhost:8080/뒤에 ?쿼리스트링이 없다. 즉 null값이다.
        // page를 위해, @RequestParam의 디폴트값을 "0"으로 지정하면
        // localhost:8080/?page=0

        // @RequestParam("") String keyword를 하지 않아도 되는 이유
        // mustache에 keyword가 null이면 URL에도 뜨지 않고 keyword가 있으면 뜨고
        // 보기 좋다.
        // @RequestParam이면 keyword가 null or 값이라도 무조건 뜬다.

        // ★ get : 매개변수 -> 쿼리스트링(body가 없음)
        // ★ post : 매개변수 -> body값

        // 페이지당 게시물 수 상수로 고정
        final int PAGESIZE = 3;

        // 검색
        // 검색을 하면 ?keyword=공백 ≠ 메인 or 일반화면에는 ?keyword=null / ?page=0
        List<Board> boardList = null;
        int boardAllSize = 0;

        // @RequestParam("") String keyword에서
        // 공백으로 받거나 아무 값을 안 받으면 디폴트값이 isblank -> 총 게시글 수
        // 공백 아닌 검색을 -> 검색 게시글 수

        if (keyword == null || keyword.trim().isEmpty()) {
            // 검색을 하지 않았으니, 기본 목록페이지를 보여주면 된다.
            boardList = boardRepository.findByAll(page);
            List<Board> boardAllList = boardRepository.findByAllBoard();
            boardAllSize = boardAllList.size();
            // 총 게시물 수
        } else {
            // 검색을 하면 그에 맞는 게시물 페이지를 보여준다.
            boardList = boardRepository.findByAll(page, keyword);
            List<Board> boardAllList = boardRepository.findByAllBoard(keyword);
            boardAllSize = boardAllList.size();
            // 검색 결과 게시물 수
        }
        // 쿼리 자체에 count를 달면, 받는 것이 int라서 size()를 굳이 하지 않아도 된다.

        System.out.println("게시글 수 : " + boardAllSize);

        request.setAttribute("boardList", boardList);
        request.setAttribute("keyword", keyword);
        request.setAttribute("nextPage", page + 1);
        request.setAttribute("prevPage", page - 1);
        request.setAttribute("first", page == 0 ? true : false);
        request.setAttribute("last",
                (boardAllSize / PAGESIZE) == page
                        || ((boardAllSize % PAGESIZE == 0) && (boardAllSize / PAGESIZE) - 1 == page) ? true
                                : false);

        return "index";
    }

    // 글쓰기 페이지
    @GetMapping("/board/saveForm")
    public String saveForm() {
        // 부가로직 ; ★ 인증페이지
        // 로그인 인증이 되면, saveForm을 실행할 수 있다.
        // 브라우저에 이동 버튼이 없어도, URL만 알다면 접근할 수 있다.(우회로 URL 접근 제한)
        User sessionUser = (User) session.getAttribute("sessionUser");
        if (sessionUser == null) {
            return "redirect:/loginForm";
        }
        // 핵심로직
        return "board/saveForm";
    }

    // 글쓰기 기능
    @PostMapping("/board/save")
    public String save(WriteDTO writeDTO) {
        // 부가로직
        // 1.로그인 인증 : 글쓰기 페이지에서 막아도, 또 POSTMAN 등으로 우회접근이 있기 때문에 모두 막는다.
        User sessionUser = (User) session.getAttribute("sessionUser");
        if (sessionUser == null) {
            return "redirect:/loginForm";
        }
        // 2. 공백, null 제한(우회 접근 제한)
        if (writeDTO.getTitle() == null || writeDTO.getTitle().isEmpty()) {
            return "redirect:/40x";
        }
        if (writeDTO.getContent() == null || writeDTO.getContent().isEmpty()) {
            return "redirect:/40x";
        }
        // 핵심로직
        boardRepository.save(writeDTO, sessionUser.getId());
        return "redirect:/";
    }

    // 게시물 상세 페이지
    // localhost:8080/board/1
    // localhost:8080/board/50
    @GetMapping("/board/{id}")
    public String detailForm(@PathVariable Integer id, HttpServletRequest request) { // C
        // 부가로직
        // 1. 게시물, 댓글 로그인 인증
        // 로그인이 되어 있고, 게시물의 id와 로그인 된 상태의 id가 같으면 권한 O, 같지 않으면 권한 X
        User sessionUser = (User) session.getAttribute("sessionUser");

        // 2. 댓글 권한 체크 및 게시물 select
        List<BoardDetailDTO> dtos = null;
        // 최초 DTO에 받은 데이터는 null
        if (sessionUser == null) {
            dtos = boardRepository.findByIdJoinReply(id, null);
            // 로그아웃 상태라면, 게시글만 보이도록 한다.
        } else {
            dtos = boardRepository.findByIdJoinReply(id, sessionUser.getId());
            // 로그인 상태라면, 1) 로그인 유저가 댓글 유저인지 확인과 2)게시글을 보이도록 한다.
            // 1) -> 댓글 유저가 아니라면 게시글만 보일 것이다.
        }

        // 3. 게시물 권한 체크
        boolean pageOwner = false;
        // 페이지의 주인은 아닌 것이 기본 값 -> null 터짐 방지
        // mustache에서 '수정/삭제'버튼의 유무를 boolean으로 줄 것이기 때문에, 디폴트값이 필요하다.

        if (sessionUser != null) {
            pageOwner = sessionUser.getId() == dtos.get(0).getBoardUserId();

            System.out.println("로그인 된 아이디 : " + sessionUser.getId());
            System.out.println("게시물 주인 : " + dtos.get(0).getBoardUserId());
        }
        // boolean pageOwner = sessionUser.getId() == board.getUser().getId();
        // sessionUser의 null 값일 때(로그아웃 상태), 오류가 터진다.

        request.setAttribute("dtos", dtos);
        request.setAttribute("pageOwner", pageOwner);
        return "board/detailForm"; //
    }

}
