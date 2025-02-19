package com.ll.codicaster.boundedContext.article.controller;

import java.util.List;
import java.util.Objects;

import com.ll.codicaster.base.rsData.RsData;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.ll.codicaster.base.rq.Rq;
import com.ll.codicaster.boundedContext.article.entity.Article;
import com.ll.codicaster.boundedContext.article.dto.ArticleDTO;
import com.ll.codicaster.boundedContext.article.service.ArticleService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/usr/article")
@RequiredArgsConstructor
public class ArticleController {

	private final ArticleService articleService;
	private final Rq rq;

	@PreAuthorize("isAuthenticated()")
	@GetMapping("/write")
	public String articleWrite() {
		return "usr/article/write";
	}

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/write")
    public String articleWriteSave(ArticleDTO articleDTO, @RequestParam("imageFile") MultipartFile imageFile) {
        RsData<Article> rsData = articleService.saveArticle(rq.getMember(), articleDTO, imageFile);
        if (rsData.isFail()) {
            return rq.historyBack(rsData);
        }
        return rq.redirectWithMsg("/usr/article/list", rsData);
    }

	@GetMapping("/list")
	public String articles(Model model) {

        List<Article> articles = articleService.articleList();
        model.addAttribute("articles", articles);

		return "usr/article/list";
	}

    @GetMapping("/detail/{id}")
	public String articleDetail(@PathVariable Long id, Model model) {

		Article article = articleService.findById(id).orElse(null);
		if (article == null) {
			return rq.historyBack("해당 게시물이 존재하지 않습니다.");
		}

		model.addAttribute("article", article);
		model.addAttribute("likeCount", article.getLikesCount());
		if (rq.isLogout()) {
			return "usr/article/detail";
		}
		model.addAttribute("isLiked", article.isLiked(rq.getMember()));
		return "usr/article/detail";
	}

	@PreAuthorize("isAuthenticated()")
	@GetMapping("/modify/{id}")
	public String modifyArticle(@PathVariable("id") Long id, Model model) {
		if (rq.isLogout()) {
			return rq.historyBack("로그인이 필요한 기능입니다.");
		}
		Article article = articleService.findById(id).orElse(null);
		if (article == null) {
			return rq.historyBack("존재하지 않는 게시물입니다.");
		}
		if (!Objects.equals(article.getAuthor().getId(), rq.getMember().getId())) {
			return rq.historyBack("작성자만 수정 가능합니다.");
		}
		model.addAttribute("article", article);
		return "usr/article/modify";
	}

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/modify/{id}")
    public String updateArticle(@PathVariable("id") Long id, ArticleDTO articleDTO, MultipartFile imageFile) {
        RsData<Article> rsData = articleService.updateArticle(rq.getMember(), id, articleDTO, imageFile);

		if (rsData.isFail()) {
			return rq.historyBack(rsData);
		}
		return rq.redirectWithMsg("/usr/article/detail/" + id, rsData);
	}

	@PreAuthorize("isAuthenticated()")
	@GetMapping("/delete/{id}")
	public String deleteArticle(@PathVariable("id") Long id) {
		RsData rsData = articleService.deleteArticle(id, rq.getMember());
		if (rsData.isFail()) {
			return rq.historyBack(rsData);
		}
		return rq.redirectWithMsg("/usr/article/list", rsData);
	}

	@GetMapping("/mylist")
	public String showMyArticle(Model model, @RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "4") int size) {
		Page<Article> articlePage = articleService.showMyList(page, size);
		List<Article> articles = articlePage.getContent();

		model.addAttribute("myArticles", articles);
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", articlePage.getTotalPages());
		model.addAttribute("mostUsedTags", rq.getMember().getMostUsedTags());

		return "usr/article/myList";
	}

	// 좋아요 추가
	@PreAuthorize("isAuthenticated()")
	@PostMapping("/like/{id}")
	public String likeArticle(@PathVariable("id") Long id) {
		RsData rsData = articleService.likeArticle(rq.getMember(), id);
		if (rsData.isFail()) {
			return rq.historyBack(rsData);
		}
		return rq.redirectWithMsg("/usr/article/detail/" + id, rsData);
	}

	// 좋아요 취소
	@PreAuthorize("isAuthenticated()")
	@PostMapping("/unlike/{id}")
	public String unlikeArticle(@PathVariable("id") Long id) {
		RsData rsData = articleService.unlikeArticle(rq.getMember(), id);
		if (rsData.isFail()) {
			return rq.historyBack(rsData);
		}
		return rq.redirectWithMsg("/usr/article/detail/" + id, rsData);
	}

	@GetMapping("/follow")
	public String showfollowingArticle(Model model, @RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "5") int size) {
		ResponseEntity<Page<Article>> response = articleService.getPageableFolloweesArticles(page, size);
		if (response.getStatusCode().is2xxSuccessful()) {
			Page<Article> articleList = response.getBody();
			model.addAttribute("articleList", articleList);
		}
		return "usr/article/followlist";
	}

	@GetMapping("/follow/list")
	public String pageableFollowsList(Model model, @RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "5") int size) {

		ResponseEntity<Page<Article>> response = articleService.getPageableFolloweesArticles(page, size);
		if (response.getStatusCode().is2xxSuccessful()) {
			Page<Article> articleList = response.getBody();
			model.addAttribute("articleList", articleList);
		}

		return "usr/article/followlist :: articleListFragment";
	}
}