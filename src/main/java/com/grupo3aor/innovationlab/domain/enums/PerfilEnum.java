package com.grupo3aor.innovationlab.domain.enums;

/**
 * Aqui decidi definir os três tipos de perfil de utilizador que vamos ter no nosso sistema.
 *
 * - ADMIN:       O nosso Administrador. É ele que gere os utilizadores e atribui os perfis.
 * - GESTOR:      O Gestor do laboratório. É quem cria as regras e analisa os alertas recebidos.
 * - UTILIZADOR:  O Utilizador padrão do sistema. Pode iniciar as simulações e consultar os seus próprios dados.
 *
 * NOTA: Mais à frente, na entidade User, uso a anotação @Enumerated para guardar o valor como texto (EnumType.STRING) na base de dados.
 * Faço isto porque fica muito mais legível quando formos ver a tabela, em vez de ficarem lá guardados os índices numéricos (0, 1, 2).
 */
public enum PerfilEnum {
    ADMIN,
    GESTOR,
    UTILIZADOR
}
