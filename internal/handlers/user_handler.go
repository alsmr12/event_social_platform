package handlers

import (
	"event_social_platform/internal/models"
	"event_social_platform/internal/repository"
	"net/http"
	"strconv"

	"github.com/gin-gonic/gin"
)

type UserHandler struct {
	userRepo *repository.UserRepository
}

func NewUserHandler(userRepo *repository.UserRepository) *UserHandler {
	return &UserHandler{userRepo: userRepo}
}

func (h *UserHandler) ShowHomePage(c *gin.Context) {
	c.HTML(http.StatusOK, "index.html", gin.H{
		"Title": "Социальная платформа",
	})
}

func (h *UserHandler) ShowCreateProfileForm(c *gin.Context) {
	c.HTML(http.StatusOK, "create_profile.html", gin.H{})
}

func (h *UserHandler) CreateProfile(c *gin.Context) {
	var req models.CreateUserRequest
	if err := c.ShouldBind(&req); err != nil {
		c.HTML(http.StatusBadRequest, "create_profile.html", gin.H{
			"Error": "Неверные данные формы",
		})
		return
	}

	// Проверяем, существует ли пользователь
	if h.userRepo.UserExists(req.Email) {
		c.HTML(http.StatusBadRequest, "create_profile.html", gin.H{
			"Error": "Пользователь с таким email уже существует",
		})
		return
	}

	// Создаем пользователя
	user := &models.User{
		Email:       req.Email,
		Password:    req.Password, // В реальном приложении нужно хешировать!
		FirstName:   req.FirstName,
		LastName:    req.LastName,
		Gender:      req.Gender,
		Age:         req.Age,
		Phone:       req.Phone,
		SocialLinks: req.SocialLinks,
	}

	if err := h.userRepo.CreateUser(user); err != nil {
		c.HTML(http.StatusInternalServerError, "create_profile.html", gin.H{
			"Error": "Ошибка при создании профиля",
		})
		return
	}

	// Перенаправляем на страницу профиля
	c.Redirect(http.StatusSeeOther, "/profile/"+strconv.Itoa(int(user.ID)))
}

func (h *UserHandler) GetProfile(c *gin.Context) {
	idStr := c.Param("id")
	id, err := strconv.Atoi(idStr)
	if err != nil {
		c.HTML(http.StatusOK, "profile.html", gin.H{
			"Error": "Неверный ID профиля",
		})
		return
	}

	user, err := h.userRepo.GetUserByID(uint(id))
	if err != nil || user == nil {
		c.HTML(http.StatusOK, "profile.html", gin.H{
			"Error": "Профиль не найден",
		})
		return
	}

	c.HTML(http.StatusOK, "profile.html", gin.H{
		"User": user,
	})
}

func (h *UserHandler) GetAllProfiles(c *gin.Context) {
	users, err := h.userRepo.GetAllUsers()
	if err != nil {
		// Всегда возвращаем Users, даже если пустой массив
		c.HTML(http.StatusOK, "profiles.html", gin.H{
			"Users": []*models.User{},
		})
		return
	}

	// Если users nil, заменяем на пустой массив
	if users == nil {
		users = []*models.User{}
	}

	c.HTML(http.StatusOK, "profiles.html", gin.H{
		"Users": users,
	})
}
