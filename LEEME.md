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

## Listas renovadas (v1.4)

En TRAKER → **Listas** ahora hay pestañas de colores que se deslizan: **Resumen · Deudas · Gastos extras · Ahorros · Diezmo · Libreta**.

- **Resumen:** estado financiero, tarjetas por tipo, alertas, gráfico de cómo se reparte el dinero y próximos recordatorios.
- **Deudas:** lo pagado y lo pendiente de cada deuda, con botón **Abonar**, avance de pagos y límite de deuda.
- **Gastos extras:** fecha de cada gasto, gráfico por mes y límite con aviso al 90 % y al superarlo.
- **Ahorros:** meta de ahorro con barra, crecimiento acumulado y ahorro por mes.
- **Libreta:** hojas rayadas, páginas, escritura rápida (con #etiquetas), recordatorios con hora, colores y notas fijadas 📌.
- En todas las listas: arrastra ⋮⋮ para ordenar, toca para editar y desliza ← para eliminar.

## Peso y cobro por libra (v1.5)

- En **Registrar pedido** (Aérea y Marítima) se quitó "Marca de ingreso" y se agregó **Peso total (lb)** y **Pago por libra (US$)**.
- Tarifas: **Aérea US$ 5.50/lb · Marítima US$ 2.00/lb** (se pueden cambiar en el campo si la tarifa cambia).
- "Dinero a pagar" se calcula en vivo: peso × tarifa.
- El peso se puede agregar o corregir después desde el detalle del pedido (cuando llegue a Miami).
- Arriba de la lista: total de libras y dólares, y el detalle **por cada marca / origen**. También en Reportes, Excel y PDF.
- BOFO ahora se llama **GOFO** (los pedidos viejos se actualizan solos).
- v1.6: nueva empresa de envío **Speedx** (formulario, filtros, reportes y cobro por libra).
- v1.7: pestaña **Buscar pedidos** (🔍) aparte de la lista principal: por cliente, fecha, empresa, peso y estado; orden por fecha, peso, costo o estado; acciones rápidas (Miami, ingreso, peso, cliente, eliminar). La búsqueda queda guardada. Nuevo campo opcional **Cliente**.
- v1.8: revisión completa del código: se quitó código sin uso, se unificaron componentes repetidos y se limpiaron imports.
- v1.9: notas con **fecha y hora** (también en la escritura rápida con el botón ⏰); el aviso llega a la hora exacta con sonido y vibración.
- v2.0: botón para agregar **Gastos necesarios** (o cualquier sección vacía) a un mes o a todos los meses; "Nuevo mes" copia cada sección del último mes que la tenga.
- v2.1: pestaña nueva **Mi Día** con tres vistas: **Hoy** (actividades como Gym que se marcan completadas o canceladas), **Temporadas** (calendario de Nicaragua con cuenta regresiva y "pide antes del…" para aérea y marítima, avisos locales) y **Premios** (metas con avance automático desde las actividades, celebración e historial). Tablas nuevas con migración; no cambia ningún dato existente.
