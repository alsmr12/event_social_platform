package handlers

import (
	"event_social_platform/internal/models"
	"event_social_platform/internal/repository"
	"net/http"

	"github.com/gin-gonic/gin"
)

type SocialHandler struct {
	socialRepo *repository.SocialLinkRepository
	userRepo   *repository.UserRepository
}

func NewSocialHandler(socialRepo *repository.SocialLinkRepository, userRepo *repository.UserRepository) *SocialHandler {
	return &SocialHandler{
		socialRepo: socialRepo,
		userRepo:   userRepo,
	}
}

func (h *SocialHandler) ShowSocialLinksForm(c *gin.Context) {
	user := GetUserFromContext(c)
	if user == nil {
		c.Redirect(http.StatusSeeOther, "/login")
		return
	}

	socialLinks, err := h.socialRepo.GetByUserID(user.ID)
	if err != nil {
		socialLinks = []*models.SocialLink{}
	}

	c.HTML(http.StatusOK, "base.html", gin.H{
		"Title":          "Социальные сети",
		"NavActive":      "social",
		"User":           user,
		"SocialLinks":    socialLinks,
		"PlatformVK":     models.PlatformVK,
		"PlatformTG":     models.PlatformTG,
		"PlatformCustom": models.PlatformCustom,
	})
}

func (h *SocialHandler) UpdateSocialLinks(c *gin.Context) {
	user := GetUserFromContext(c)
	if user == nil {
		c.Redirect(http.StatusSeeOther, "/login")
		return
	}

	if err := h.socialRepo.DeleteByUserID(user.ID); err != nil {
		c.HTML(http.StatusInternalServerError, "base.html", gin.H{
			"Title":     "Ошибка",
			"NavActive": "social",
			"Error":     "Ошибка при обновлении социальных сетей",
		})
		return
	}

	platforms := c.PostFormArray("platform[]")
	usernames := c.PostFormArray("username[]")
	customNames := c.PostFormArray("custom_name[]")

	for i, platform := range platforms {
		if platform != "" && i < len(usernames) && usernames[i] != "" {
			socialLink := &models.SocialLink{
				UserID:     user.ID,
				Platform:   platform,
				Username:   usernames[i],
				CustomName: customNames[i],
			}
			if err := h.socialRepo.Create(socialLink); err != nil {
				c.HTML(http.StatusInternalServerError, "base.html", gin.H{
					"Title":     "Ошибка",
					"NavActive": "social",
					"Error":     "Ошибка при сохранении социальных сетей",
				})
				return
			}
		}
	}

	c.Redirect(http.StatusSeeOther, "/profile")
}
