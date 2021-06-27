package com.project.myver.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import com.project.myver.dto.BlogDTO;
import com.project.myver.dto.CommentDTO;
import com.project.myver.dto.FileDTO;
import com.project.myver.dto.MemberDTO;
import com.project.myver.dto.MemoDTO;
import com.project.myver.service.ImageService;
import com.project.myver.service.MemberService;
import com.project.myver.util.PageUtil;
import com.project.myver.service.BlogService;

@Controller
@RequestMapping("/blog")
public class BlogController {
	
	@Autowired
	private BlogService blogSVC;
	@Autowired
	private MemberService memSVC;
	@Autowired
	private ImageService imgSVC;
	
	// 21.05.17 블로그 홈_메인 페이지
	@RequestMapping(value = "/home")
	public ModelAndView blogHome(HttpSession session, ModelAndView mv) {
		/* main.jsp에 보낼 정보
		 * 1. 블로그 정보 'blog'table - blog_no, nick, blog_img_no
		 * 2. blog_img_no로 'image'table에서 path, saved_name 가져오기
		 * 3. 오늘 방문자수 'blog_visit'table
		 * 4. 내 소식 리스트
		 * 5. 내가 남긴 글 리스트
		 * 6. 이웃 목록 리스트
		 * 7. 이웃 새글 리스트
		 * 8. 인기글 리스트 가져오기
		 */
		
		if(session.getAttribute("MID")!=null) {
			int member_no = memSVC.selectMember_noById((String)session.getAttribute("MID"));
			
			// 1. 블로그 정보 'blog'table - blog_no, nick, blog_img_no
			BlogDTO blogDTO = blogSVC.selectBlogHomeDataFromBlog(member_no);
			// 2. blog_img_no로 'image'table에서 path, saved_name 가져오기
			
			// 3. 오늘 방문자수 'blog_visit'table
			int today_visit_cnt = blogSVC.todayBlogVisitCount(blogDTO.getBlog_no());
			
			// 4. 내 소식 리스트
			// 5. 내가 남긴 글 리스트
			// 6. 이웃 목록 리스트
			// 7. 이웃 새글 리스트
			// 8. 인기글 리스트 가져오기
			mv.addObject("BLOG", blogDTO);
			mv.addObject("TODAYVISIT", today_visit_cnt);
		}
		
		mv.setViewName("blog/home");
		
		return mv;
	}
	
	// 블로그 홈_메인 페이지
	@RequestMapping(value = "/")
	public ModelAndView blogHome2(HttpSession session, ModelAndView mv) {
		return blogHome(session, mv);
	}
	
	// 21.05.19 블로그 메인
	// ★★★★★ 댓글, 좋아요 기능 추가해야함!
	@RequestMapping(value = "/{blog_id}")
	public ModelAndView blogMain(HttpSession session, 
				     ModelAndView mv, 
				     @PathVariable("blog_id")String blog_id, 
				     @RequestParam(value="query", defaultValue="") String query, 
				     @RequestParam(value="currentPage", defaultValue="1") int currentPage,
				     @RequestParam(value="blog_category_no", defaultValue="-1") int blog_category_no) {
		// "myver/blog/"로 들어온 경우 블로그 홈으로 이동
		if(blog_id.length() == 0) { 
			mv.setViewName("blog/home");
			return mv;
		}
		
		// 블로그 주인 회원 번호
		int blog_member_no = memSVC.selectMember_noById(blog_id);
		
		if(blog_member_no == 0) { // member id(blogId)가 존재하지 않는 경우 (혹은 member id가 관리자인 경우)
			System.out.println("blog id가 존재하지 않음");
			mv.setViewName("blog/no_blog");
			return mv;
		}
		
		// 21.05.19 member_no(blog_id)로 블로그 정보 가져오기
		BlogDTO blogDTO = blogSVC.selectAllFromBlog(blog_id);
		
		//방문자
		String visitor_id = (String)session.getAttribute("MID");
		int visitor_no = -1;
		
		// 방문자 회원 번호
		if(visitor_id != null) {
			visitor_no = memSVC.selectMember_noById(visitor_id);
		}
		
		// 방문자가 주인인지 여부(주인일 경우:true, 외부인일 경우:false)
		boolean is_owner = blogDTO.getMember_no() == visitor_no;
		
		// 21.06.12  블로그 메인 - 카테고리 리스트, 목록 리스트, 글 리스트 가져오기
		Map<String,Object> map  = blogSVC.selectCategoryAndListAndObject(blogDTO, is_owner, blog_category_no, currentPage);
		
		// 21.06.12 가져올 카테고리가 없는 경우, 해당하는 블로그 메인으로 이동
		if(map == null) {
			System.out.println("해당 카테고리를 방문하는데 오류가 발생했습니다.");
			mv.setViewName("blog/error_to_main");
			mv.addObject("BLOG", blogDTO);
			return mv;
		}

		// 21.06.09 외부인이 방문한 경우, 블로그 방문자 정보 추가
		if(!is_owner) {
			blogSVC.insertBlog_visit(blogDTO.getBlog_no(), -1, visitor_no, query, session);
		}
		
		// 21.05.24 이웃 리스트 가져오기 (내가 추가한 이웃 following / 나를 추가한 이웃 follower)
		List<BlogDTO> followingList = blogSVC.selectFollowingListFromBlog_neighbor(blog_member_no);
		List<BlogDTO> followerList = blogSVC.selectFollowerListFromBlog_neighbor(blog_member_no);
		
		mv.addObject("BLOG", blogDTO);
		mv.addObject("FOLLOWING", followingList);
		mv.addObject("FOLLOWER", followerList);

		mv.addObject("CATEGORY_LIST", (List)map.get("categoryList"));
		mv.addObject("CATEGORY", map.get("blog_category"));
		mv.addObject("CATEGORY_TOTAL", map.get("totalCount"));
		mv.addObject("LIST", (List)map.get("lists"));
		mv.addObject("OBJECTS", (List)map.get("objects"));

		mv.setViewName("blog/main");
		
		return mv;
	}
	
	// ★★★★★ 댓글, 좋아요 기능 추가해야함!
    // 21.05.30 블로그 글 보기	
    @RequestMapping(value = "/{blog_id}/{blog_object_no}")	
    public ModelAndView blogObject(HttpSession session, 
				   ModelAndView mv, 
				   @PathVariable("blog_id") String blog_id, 
				   @PathVariable("blog_object_no") int blog_object_no, 
				   @RequestParam(value="query", defaultValue="") String query,
				   @RequestParam(value="blog_category_no", defaultValue="-1") int blog_category_no) {	
		if(blog_id.length() == 0) {
			mv.setViewName("blog/home");
			return mv;
		}
		
		// 블로그 주인 회원 번호
		int blog_member_no = memSVC.selectMember_noById(blog_id);
		
		// 블로그가 존재하지 않는 경우 <- member id(blog_member_no)가 존재하지 않는 경우(혹은 member id가 관리자인 경우)
		if(blog_member_no == 0) { 
			System.out.println("member id == 0");
			mv.setViewName("blog/no_blog");
			return mv;
		}
		
		//방문자 id
		String visitor_id = (String)session.getAttribute("MID");
		
		int visitor_no = -1;
		
		// 방문자 member_no
		if(visitor_id != null) {
			visitor_no = memSVC.selectMember_noById(visitor_id);
		}
		
		// blog_id로 블로그 정보 가져오기
		BlogDTO blogDTO = blogSVC.selectAllFromBlog(blog_id);
		
		// 21.06.10 'blog_no'와 'blog_object_no'에 일치하는 'blog_object' 가져오기
		boolean is_owner = blog_member_no == visitor_no; // 블로그 주인이면 해당 글이 비공개여도 가져올 수 있다!
		BlogDTO object = blogSVC.selectBlog_object(blogDTO.getBlog_no(), blog_object_no, is_owner);
		
		// 해당 블로그에 해당 게시글이 존재하지 않는 경우
		if(object == null) {
			System.out.println("해당 블로그에 존재하지 않는 게시글입니다.");
			mv.setViewName("blog/no_blog_object");
			mv.addObject("BLOG", blogDTO);
			return mv;
		}
		
		// 카테고리 리스트 가져오기
		
		// 21.06.14 블로그 글 보기 - 카테고리, 목록 가져오기
		Map<String,Object> map  = blogSVC.selectCategoryAndList(blogDTO, object, is_owner, blog_category_no);
		
		// 가져올 카테고리가 없는 경우, 해당하는 블로그 메인으로 이동
		if(map == null) {
			System.out.println("해당 카테고리를 방문하는데 오류가 발생했습니다.");
			mv.setViewName("blog/error_to_main");
			mv.addObject("BLOG", blogDTO);
			return mv;
		}
		
		// 외부인이 방문한 경우
		if(!is_owner) { 
			// 21.06.09 게시글 조회수 업데이트(증가)
			blogSVC.updateBlogObjectHits(blog_object_no, session);
			
			// 21.06.09 블로그 방문자 정보 추가
			blogSVC.insertBlog_visit(blogDTO.getBlog_no(), blog_object_no, visitor_no, query, session);
		}
		
		// 21.06.04 이웃 리스트 가져오기 (내가 추가한 이웃 following / 나를 추가한 이웃 follower)
		List<BlogDTO> followingList = blogSVC.selectFollowingListFromBlog_neighbor(blog_member_no);
		List<BlogDTO> followerList = blogSVC.selectFollowerListFromBlog_neighbor(blog_member_no);
	
		mv.addObject("BLOG", blogDTO);
		mv.addObject("FOLLOWING", followingList);
		mv.addObject("FOLLOWER", followerList);
		mv.addObject("OBJECT", object);
		
		mv.addObject("CATEGORY_LIST", (List)map.get("categoryList"));
		mv.addObject("CATEGORY", map.get("blog_category"));
		mv.addObject("CATEGORY_TOTAL", map.get("totalCount"));
		mv.addObject("LIST", (List)map.get("lists"));
		
		mv.setViewName("blog/object");
		
		return mv;
    }

    // 21.06.15 블로그 관리 페이지
    @RequestMapping(value = "/admin/{blog_id}/{menu}")	
    public ModelAndView blogConfig(HttpSession session, 
				   ModelAndView mv, 
				   @PathVariable("blog_id") String blog_id,
				   @PathVariable("menu") String menu) {
    	String visitor_id = (String)session.getAttribute("MID");
    	
    	if(visitor_id==null || !blog_id.equals(visitor_id)) {
    		// ★★★★★★ 로그인 창으로 보내기~ ★★★★★★★★★★★★★★★★★★★★★★★★
    		
    	}
    	
    	BlogDTO blogDTO = blogSVC.selectAllFromBlog(blog_id);
		int blog_member_no = blogDTO.getMember_no();
		int blog_no = blogDTO.getBlog_no();
    	
    	// 21.06.16 menu - config(기본 설정)
    	if(menu==null || menu.length()==0 || menu.equals("config")) {
    		// 1) 블로그 정보 : blog_id로 'blog' 정보 가져오기 <- 위에서 이미 가져옴
    		
    		// 2) 내가 추가한 이웃 : 내가 추가한 이웃 리스트 가져오기
    		List<BlogDTO> followingList = blogSVC.selectFollowingListFromBlog_neighbor(blog_member_no);
    		
    		// 3) 나를 추가한 이웃 : 나를 추가한 이웃 리스트 가져오기
    		List<BlogDTO> followerList = blogSVC.selectFollowerListFromBlog_neighbor(blog_member_no);

    		for(BlogDTO b : followerList) {
    			System.out.println(b.toString());
    		}
    		mv.addObject("FOLLOWINGS", followingList);
    		mv.addObject("FOLLOWERS", followerList);
            mv.setViewName("blog/admin/config");
    	
		// 21.06.19 menu - content(메뉴,글,동영상 관리)
    	}else if(menu.equals("content")) {
    		// 1) 상단 메뉴 설정 
     	    // 2) 카테고리 설정
    		// -> 카테고리 리스트 가져오기
    		List<BlogDTO> categoryList = blogSVC.selectAllFromBlog_category(blog_no);
    		
     	    // 3) 게시글 관리 -> 글 목록 가져오기
    		// ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★ 
    		//  1 - 페이지에 따라 비동기로 가져오는거 처리해야함
    		//  2 - 아이디로 검색하는것도 비동기 처리해야함
    		int totalCount = blogSVC.selectTotalCountByNoFromBlog_object(blog_no, "blog_no", true);
    		PageUtil pageInfo = new PageUtil(1,totalCount,20,blog_no,"blog_no",true);
    		List<BlogDTO> objectList = blogSVC.selectObjectDetailByNoFromBlog_object(pageInfo);
     	    
    		// 4) 댓글 관리 -> 모든 	댓글 가져오기
    		List<CommentDTO> commentList = blogSVC.selectCommentByBlog_noFromBlog_comment(blog_no);
    		
    		mv.addObject("CATEGORYS", categoryList);
    		mv.addObject("OBJECTS", objectList);
    		mv.addObject("COMMENTS", commentList);
            mv.setViewName("blog/admin/content");

            for(BlogDTO category : categoryList) {
            	System.out.println(category.blog_categoryToString());
            }
            System.out.println("===========");
            for(BlogDTO object : objectList) {
            	System.out.println(object.blog_objectToString());
            }
            System.out.println("===========");
            for(CommentDTO category : commentList) {
            	System.out.println(category.toString());
            }
            System.out.println("===========");
		// menu - stat(내 블로그 통계)
    	}else if(menu.equals("stat")) {
    		
    	}
    	
    	/* menu가 없으면 basic_setting jsp페이지로~
    	 * menu에 맞게 보내기
    	 * 종류) config(기본 설정)/content(메뉴,글 관리)/stat(내 블로그 통계)
    	   - config(기본 설정)
    	   1) 블로그 정보 : blog_id로 'blog' 정보 가져오기
    	   2) 내가 추가한 이웃 : 내가 추가한 이웃 리스트 가져오기
    	   3) 나를 추가한 이웃 : 나를 추가한 이웃 리스트 가져오기
    	   4) 블로그 초기화
    	   
    	   - content(메뉴,글 관리)
    	   1) 상단 메뉴 설정
    	   2) 카테고리 설정
    	   3) 게시글 관리
    	   4) 댓글 관리
    	   
    	   - stat(내 블로그 통계) ----> 이건 한번에 다 가져오지 말고 그때그때 가져와야 할 것 같다.
    	   0) 오늘 (/today)
	      - 날짜 받아오기(없는 경우 오늘 날짜)
    	   1) 조회수 (/visit_pv) ~~ 글 조회수
	      - 일간(데이터 없는 경우, 일간-오늘) / 주간 / 월간
	      - 전체/이웃/기타
    	   2) 방문 횟수 (/visit) ~~ 블로그 방문 횟수
	      - 일간(데이터 없는 경우, 일간-오늘) / 주간 / 월간
	      - 전체/이웃/기타
    	   3) 검색어 분석(/referer)
	      - 일간(데이터 없는 경우, 일간-오늘) / 주간 / 월간
    	   4) 시간대 분석 (/hour_cv)--할지말지 고민중~~~
    	   5) 성별·연령별 분포 (/demo) --이것두 고민중~~~
    	   6) 조회수 순위 (/rank_pv)
	      - 일간(데이터 없는 경우, 일간-오늘) / 주간 / 월간
    	   7) 좋아요수 순위 (/rank_likes)
	      - 일간(데이터 없는 경우, 일간-오늘) / 주간 / 월간
    	   8) 댓글수 순위 (/rank_comment)
	      - 일간(데이터 없는 경우, 일간-오늘) / 주간 / 월간
    	 */
    	mv.addObject("BLOG", blogDTO);
    	
    	return mv;
    }
    
    // 21.06.21 블로그 정보 수정
    @RequestMapping(value = "/admin/updateBlog", method = RequestMethod.POST)
	@ResponseBody
	public String updateBlog(BlogDTO blogDTO) {
		System.out.println("updateBlog - blogDTO: "+blogDTO.blogToString());
		String result = "";
		
		/* ★★★ 업로드한 이미지가 있는 경우 이미지 업로드...하고 번호 가져와서 blogDTO에 set ★★★ */
		//boolean imgCange = false;
		
		int cnt = blogSVC.blogUpdate(blogDTO);
		
		if(cnt==1) {
			System.out.println("updateBlog - 데이터 업데이트 성공");
			result = "success";
		}else {
			System.out.println("updateBlog - cnt: "+cnt);
			result = "fail";
		}
		
		return result;
	}
}
