package models

// RegisterRequest — запрос на регистрацию (для JSON API)
type RegisterRequest struct {
	FirstName string `json:"first_name" binding:"required"`
	LastName  string `json:"last_name" binding:"required"`
	Email     string `json:"email" binding:"required,email"`
	Password  string `json:"password" binding:"required,min=6"`
	Gender    string `json:"gender" binding:"required"`
	BirthDate string `form:"birth_date"`
	Phone     string `json:"phone" binding:"required"`
}
