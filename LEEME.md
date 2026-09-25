# Keni's Logística — App Android

App de logística y control de mercadería (Aérea / Marítima) para Keni's Shop.
Todo se guarda **en el teléfono o tablet** (base de datos Room/SQLite). No usa internet ni servidor.

- Login administrador: usuario `admin` · contraseña `admin`
- Android 8.0 o superior, teléfono y tablet (en tablet: lista a la izquierda, formulario/detalle a la derecha)

---

## Opción A — Obtener el APK con link de descarga (sin instalar nada en tu PC)

1. Entra a https://github.com y crea un repositorio nuevo (ej. `kenis-logistica`).
2. Descomprime este ZIP y sube **todo el contenido** de la carpeta `KenisLogistica`
   (incluida la carpeta `.github`). Lo más fácil es con **GitHub Desktop**
   o con "Add file → Upload files" arrastrando todas las carpetas.
3. Ve a la pestaña **Actions**: se ejecuta sola "Compilar APK" (tarda ~5–8 minutos).
4. Cuando termine en verde ✅, ve a **Releases** (columna derecha del repositorio):
   ahí está el archivo `KenisLogistica-v1.0.X.apk` → ese es tu **link de descarga**.
5. Abre ese link en el teléfono, descarga el APK e instálalo
   (Android pedirá permitir "instalar apps de origen desconocido": acéptalo).

Cada vez que subas un cambio al repositorio se genera un APK nuevo.
Como la app está firmada siempre con la misma llave (`app/kenis-logistica.jks`),
la versión nueva se instala **encima** de la anterior **sin borrar los pedidos**.
⚠️ No borres ni cambies ese archivo `.jks`.

> Si el repositorio es privado, para descargar hay que iniciar sesión en GitHub.
> Si es público, cualquiera con el link puede descargar el APK (los datos NO se comparten: viven en cada teléfono).

## Opción B — Android Studio

1. Abre Android Studio → **Open** → selecciona la carpeta `KenisLogistica`.
2. Espera que sincronice Gradle.
3. Menú **Build → Build App Bundle(s) / APK(s) → Build APK(s)**,
   o conecta el teléfono por USB y presiona ▶ Run.

---

## Cómo funciona

| Etapa | Qué se registra | Estado |
|---|---|---|
| 1. Realización del pedido | código, foto, marca, origen, empresa a Miami, fecha | 🟡 Pendiente |
| 2. Llegada al casillero Miami | botón "Marcar llegada a Miami" | 🔵 En tránsito |
| 3. Ingreso final | botón "Registrar ingreso" | 🟢 Ingresado |
| Sin ingreso en 15 días (aérea) / 25 días (marítima) | automático | 🔴 Vencido |

- **Notificaciones** (funcionan con la app cerrada y se reprograman al reiniciar el teléfono):
  - Alerta de atraso por cada pedido vencido (WorkManager, revisión diaria).
  - Recordatorio diario a las 6:00 pm: "¿Se registró mercadería hoy?" con la lista de pedidos pendientes (AlarmManager).
  - Para cambiar la hora: `notif/Programador.kt` → `HORA_RECORDATORIO`.
- **Edición limitada:** el código y los datos originales no se modifican; solo se edita la fecha de ingreso.
- **Sin eliminación automática:** los pedidos nunca se borran.
- **Reportes:** filtros por fecha, tipo, empresa y estado → exporta a **Excel (.xlsx)** o **PDF** y se comparte/guarda (WhatsApp, Drive, correo, Archivos…).
- **Modo oscuro** automático según el sistema.

---

## Pestaña TRAKER (v1.1)

Control de gastos que se alimenta de tu Excel **TRAKER DE GASTO**.

1. Pestaña **TRAKER** → **Importar Excel** → elige el `.xlsx` → confirma el año → **Importar**.
2. La app reconoce sola los bloques del Excel: cada mes con *Gastos personales*, *Kenisshop* y *Gastos necesarios*
   (categoría, presupuesto, gasto real y capital), además de *Deudas*, *Gastos extras*, *Ahorros*, *Diezmo* y *Notas*.
3. Desde ahí ya no se usa el Excel: todo se edita en la app.

Vistas:
- **Mes**: desliza a los lados para cambiar de mes. Resumen, tarjetas por sección con barras de avance,
  capital y saldo (toca para editarlo), gráfico circular y barras. "Nuevo mes" copia los presupuestos del mes anterior.
- **Registros**: búsqueda y filtros por mes, sección, categoría y estado (Sin gasto / Dentro / Cerca del límite / Excedido).
- **Gráficos**: líneas por mes, presupuesto vs gasto, distribución por categoría y ahorros (toca para ver detalles).
- **Listas**: deudas, gastos extras, ahorros, diezmo y notas.

Tocar un registro = editar (con botón **Sumar** para agregar el gasto del día). Deslizar a la izquierda = eliminar.

Notificaciones automáticas: al importar (qué cambió respecto a lo anterior) y cuando una categoría
pasa el 90 % o supera su presupuesto, o una sección queda con saldo negativo.

Los datos del Traker van en una base aparte (`traker.db`): los pedidos no se tocan.
