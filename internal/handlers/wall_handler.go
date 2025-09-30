package handlers

import (
	"event_social_platform/internal/models"
	"event_social_platform/internal/repository"
	"net/http"
	"strconv"

	"github.com/gin-gonic/gin"
)

type WallHandler struct {
	wallRepo *repository.WallRepository
	userRepo *repository.UserRepository
}

func NewWallHandler(wallRepo *repository.WallRepository, userRepo *repository.UserRepository) *WallHandler {
	return &WallHandler{
		wallRepo: wallRepo,
		userRepo: userRepo,
	}
}

func (h *WallHandler) CreatePost(c *gin.Context) {
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.Redirect(http.StatusSeeOther, "/login")
		return
	}

	var req models.CreateWallPostRequest
	if err := c.ShouldBind(&req); err != nil {
		c.HTML(http.StatusBadRequest, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "",
			"Error":       "Неверные данные формы: " + err.Error(),
			"CurrentUser": currentUser,
		})
		return
	}

	// Проверяем, существует ли пользователь, на чью стену пишем
	_, err := h.userRepo.GetUserByID(req.UserID)
	if err != nil {
		c.HTML(http.StatusNotFound, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "",
			"Error":       "Пользователь не найден",
			"CurrentUser": currentUser,
		})
		return
	}

	post := &models.WallPost{
		Content:  req.Content,
		AuthorID: currentUser.ID,
		UserID:   req.UserID,
	}

	if err := h.wallRepo.CreatePost(post); err != nil {
		c.HTML(http.StatusInternalServerError, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "",
			"Error":       "Ошибка создания записи",
			"CurrentUser": currentUser,
		})
		return
	}

	// ВСЕГДА возвращаем на профиль владельца стены
	c.Redirect(http.StatusSeeOther, "/profile/"+strconv.Itoa(int(req.UserID)))
}

func (h *WallHandler) DeletePost(c *gin.Context) {
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.Redirect(http.StatusSeeOther, "/login")
		return
	}

	idStr := c.Param("id")
	id, err := strconv.Atoi(idStr)
	if err != nil {
		c.HTML(http.StatusBadRequest, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "",
			"Error":       "Неверный ID записи",
			"CurrentUser": currentUser,
		})
		return
	}

	// Проверяем права на удаление
	canDelete, err := h.wallRepo.CanDeletePost(uint(id), currentUser.ID)
	if err != nil || !canDelete {
		c.HTML(http.StatusForbidden, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "",
			"Error":       "Нет прав для удаления этой записи",
			"CurrentUser": currentUser,
		})
		return
	}

	if err := h.wallRepo.DeletePost(uint(id)); err != nil {
		c.HTML(http.StatusInternalServerError, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "",
			"Error":       "Ошибка удаления записи",
			"CurrentUser": currentUser,
		})
		return
	}

	// Возвращаем на предыдущую страницу
	referer := c.Request.Header.Get("Referer")
	if referer == "" {
		referer = "/"
	}
	c.Redirect(http.StatusSeeOther, referer)
}

func (h *WallHandler) ShowEditForm(c *gin.Context) {
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.Redirect(http.StatusSeeOther, "/login")
		return
	}

	idStr := c.Param("id")
	id, err := strconv.Atoi(idStr)
	if err != nil {
		c.HTML(http.StatusBadRequest, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "",
			"Error":       "Неверный ID записи",
			"CurrentUser": currentUser,
		})
		return
	}

	// Получаем запись
	post, err := h.wallRepo.GetPostByID(uint(id))
	if err != nil {
		c.HTML(http.StatusNotFound, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "",
			"Error":       "Запись не найдена",
			"CurrentUser": currentUser,
		})
		return
	}

	// Проверяем права на редактирование
	canEdit, err := h.wallRepo.CanEditPost(uint(id), currentUser.ID)
	if err != nil || !canEdit {
		c.HTML(http.StatusForbidden, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "",
			"Error":       "Нет прав для редактирования этой записи",
			"CurrentUser": currentUser,
		})
		return
	}

	c.HTML(http.StatusOK, "base.html", gin.H{
		"Title":       "Редактирование записи",
		"NavActive":   "edit_post", // ИСПРАВЛЕНО (было пусто)
		"Post":        post,
		"CurrentUser": currentUser,
	})
}

func (h *WallHandler) UpdatePost(c *gin.Context) {
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.Redirect(http.StatusSeeOther, "/login")
		return
	}

	idStr := c.Param("id")
	id, err := strconv.Atoi(idStr)
	if err != nil {
		c.HTML(http.StatusBadRequest, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "",
			"Error":       "Неверный ID записи",
			"CurrentUser": currentUser,
		})
		return
	}

	// Проверяем права на редактирование
	canEdit, err := h.wallRepo.CanEditPost(uint(id), currentUser.ID)
	if err != nil || !canEdit {
		c.HTML(http.StatusForbidden, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "",
			"Error":       "Нет прав для редактирования этой записи",
			"CurrentUser": currentUser,
		})
		return
	}

	var req struct {
		Content string `form:"content" binding:"required,max=1000"`
	}

	if err := c.ShouldBind(&req); err != nil {
		c.HTML(http.StatusBadRequest, "base.html", gin.H{
			"Title":       "Редактирование записи",
			"NavActive":   "",
			"Error":       "Неверные данные формы",
			"Post":        &models.WallPost{ID: uint(id)},
			"CurrentUser": currentUser,
		})
		return
	}

	// Получаем запись и обновляем содержимое
	post, err := h.wallRepo.GetPostByID(uint(id))
	if err != nil {
		c.HTML(http.StatusNotFound, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "",
			"Error":       "Запись не найдена",
			"CurrentUser": currentUser,
		})
		return
	}

	post.Content = req.Content

	if err := h.wallRepo.UpdatePost(post); err != nil {
		c.HTML(http.StatusInternalServerError, "base.html", gin.H{
			"Title":       "Редактирование записи",
			"NavActive":   "",
			"Error":       "Ошибка обновления записи",
			"Post":        post,
			"CurrentUser": currentUser,
		})
		return
	}

	// ВСЕГДА возвращаем на профиль владельца стены
	c.Redirect(http.StatusSeeOther, "/profile/"+strconv.Itoa(int(post.UserID)))
}
