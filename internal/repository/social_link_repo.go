package repository

import (
	"event_social_platform/internal/models"
	"gorm.io/gorm"
)

type SocialLinkRepository struct {
	db *gorm.DB
}

func NewSocialLinkRepository(db *gorm.DB) *SocialLinkRepository {
	return &SocialLinkRepository{db: db}
}

func (r *SocialLinkRepository) Create(socialLink *models.SocialLink) error {
	return r.db.Create(socialLink).Error
}

func (r *SocialLinkRepository) GetByUserID(userID uint) ([]*models.SocialLink, error) {
	var socialLinks []*models.SocialLink
	err := r.db.Where("user_id = ?", userID).Find(&socialLinks).Error
	if err != nil {
		return nil, err
	}
	return socialLinks, nil
}

func (r *SocialLinkRepository) DeleteByUserID(userID uint) error {
	return r.db.Where("user_id = ?", userID).Delete(&models.SocialLink{}).Error
}
