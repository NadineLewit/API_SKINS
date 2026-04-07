{
  "_type": "export",
  "__export_format": 4,
  "__export_date": "2026-04-07T00:00:00.000Z",
  "__export_source": "skins-marketplace-api-fixed",
  "resources": [
    {
      "_id": "wrk_skins",
      "_type": "workspace",
      "name": "Skins Marketplace API",
      "scope": "collection"
    },
    {
      "_id": "env_base",
      "_type": "environment",
      "name": "Base Environment",
      "data": {
        "base_url": "http://localhost:4002",
        "token_user": "",
        "token_admin": ""
      },
      "parentId": "wrk_skins"
    },

    {
      "_id": "fld_auth",
      "_type": "request_group",
      "name": "🔐 Auth",
      "parentId": "wrk_skins"
    },

    {
      "_id": "req_login",
      "_type": "request",
      "name": "Login",
      "method": "POST",
      "url": "{{ base_url }}/api/v1/auth/authenticate",
      "headers": [{ "name": "Content-Type", "value": "application/json" }],
      "body": {
        "mimeType": "application/json",
        "text": "{ \"email\": \"juan@mail.com\", \"password\": \"pass123\" }"
      },
      "parentId": "fld_auth"
    },

    {
      "_id": "fld_orders",
      "_type": "request_group",
      "name": "📦 Orders",
      "parentId": "wrk_skins"
    },

    {
      "_id": "req_delete_order",
      "_type": "request",
      "name": "Eliminar orden — DELETE",
      "method": "DELETE",
      "url": "{{ base_url }}/order/1",
      "headers": [{ "name": "Authorization", "value": "Bearer {{ token_user }}" }],
      "parentId": "fld_orders"
    },

    {
      "_id": "fld_skins_extra",
      "_type": "request_group",
      "name": "🎮 Skins EXTRA",
      "parentId": "wrk_skins"
    },

    {
      "_id": "req_create_skin_with_image",
      "_type": "request",
      "name": "Crear skin con imagen [ADMIN]",
      "method": "POST",
      "url": "{{ base_url }}/skins/admin/create-with-image",
      "headers": [
        { "name": "Authorization", "value": "Bearer {{ token_admin }}" }
      ],
      "body": {
        "mimeType": "multipart/form-data",
        "params": [
          { "name": "name", "value": "AK-47 | Redline" },
          { "name": "price", "value": "25.5" },
          { "name": "stock", "value": "3" },
          { "name": "game", "value": "CS2" },
          { "name": "categoryId", "value": "1" },
          { "name": "imagen", "type": "file", "fileName": "" }
        ]
      },
      "parentId": "fld_skins_extra"
    },

    {
      "_id": "req_edit_skin_with_image",
      "_type": "request",
      "name": "Editar skin con imagen [ADMIN]",
      "method": "PUT",
      "url": "{{ base_url }}/skins/admin/1/edit-with-image",
      "headers": [
        { "name": "Authorization", "value": "Bearer {{ token_admin }}" }
      ],
      "body": {
        "mimeType": "multipart/form-data",
        "params": [
          { "name": "skin", "value": "{ \"name\": \"Updated Skin\" }" },
          { "name": "image", "type": "file", "fileName": "" }
        ]
      },
      "parentId": "fld_skins_extra"
    },

    {
      "_id": "fld_admin_extra",
      "_type": "request_group",
      "name": "🛡️ Admin EXTRA",
      "parentId": "wrk_skins"
    },

    {
      "_id": "req_admin_get_cupon",
      "_type": "request",
      "name": "Get cupón por ID [ADMIN]",
      "method": "GET",
      "url": "{{ base_url }}/api/v1/admin/cupones/1",
      "headers": [
        { "name": "Authorization", "value": "Bearer {{ token_admin }}" }
      ],
      "parentId": "fld_admin_extra"
    }
  ]
}