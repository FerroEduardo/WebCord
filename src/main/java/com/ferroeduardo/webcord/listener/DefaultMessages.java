package com.ferroeduardo.webcord.listener;

public enum DefaultMessages {
    USER_IS_NOT_ADMIN("Você não é um administrador para realizar essa ação"),
    NO_WEBSITE_REGISTERED("Nenhum site foi cadastrado no bot"),
    WAIT_A_MOMENT("Aguarde um momento..."),
    FAILED_TO_REMOVE_CHANNEL("Ocorreu uma falha ao remover o canal"),
    CHANNEL_REMOVED_WITH_SUCCESS("Canal removido com sucesso"),
    CHANNEL_NOT_REGISTERED_TO_BE_REMOVED("Parece que esse canal não está cadastrado para poder ser removido"),
    CONFIGURED_WITH_SUCCESS("Configurado com sucesso"),
    ALREADY_ADDED_CHANNEL("Canal já foi adicionado anteriormente");

    private String message;

    DefaultMessages(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
