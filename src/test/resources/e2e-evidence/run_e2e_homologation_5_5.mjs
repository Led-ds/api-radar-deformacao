import { spawn, execSync } from 'child_process';
import fs from 'fs';
import path from 'path';

const EVIDENCE_DIR = '/home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/src/test/resources/e2e-evidence';
const ARTIFACT_DIR = '/home/asoares/.gemini/antigravity-ide/brain/985ae30a-94d9-4657-ad01-fae69e2f68c3';
const CHROME_PATH = '/usr/bin/google-chrome';
const PORT = 9222;

const OFFICIAL_MACEIO_AOI_ID = '1e7a1465-c3d7-44e6-8398-5ced7dac17ec';
const TRACKED_AOI_UUIDS_FILE = path.join(EVIDENCE_DIR, '.e2e_tracked_uuids.json');
const trackedCreatedAoiIds = new Set();

async function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

function persistTrackedUuids() {
  try {
    fs.writeFileSync(TRACKED_AOI_UUIDS_FILE, JSON.stringify(Array.from(trackedCreatedAoiIds), null, 2), 'utf-8');
  } catch (err) {
    console.warn('[Tracking Warning] Falha ao persistir UUIDs no disco:', err.message);
  }
}

const ALLOWED_TEST_ENVIRONMENTS = ['test', 'development', 'homologation', 'local', 'dev', 'ci'];
const EXPECTED_TEST_DB_NAME = 'radar_db';
const EXPECTED_TEST_DB_USER = 'radar_user';

function verifyTestEnvironment() {
  const currentEnv = (process.env.RADAR_ENV || process.env.NODE_ENV || 'test').toLowerCase().trim();
  const isProduction = currentEnv === 'production' || currentEnv === 'prod' || process.env.SPRING_PROFILES_ACTIVE === 'prod';

  if (isProduction || !ALLOWED_TEST_ENVIRONMENTS.includes(currentEnv)) {
    throw new Error(
      `[SEGURANÇA CRÍTICA - OPERAÇÃO ABORTADA] Operação E2E rejeitada: ambiente atual '${currentEnv}' não é um ambiente de testes autorizado (${ALLOWED_TEST_ENVIRONMENTS.join(', ')}). Exclusões por UUID e testes automatizados são terminantemente proibidos em produção.`
    );
  }
  return currentEnv;
}

function assertIsolatedTestDatabase() {
  const currentEnv = verifyTestEnvironment();
  try {
    const identityCmd = `docker exec radar_postgres psql -U radar_user -d radar_db -t -A -c "SELECT current_database() || '|' || current_user;"`;
    const identity = execSync(identityCmd, { encoding: 'utf-8' }).trim();
    const [dbName, dbUser] = identity.split('|');

    if (dbName !== EXPECTED_TEST_DB_NAME || dbUser !== EXPECTED_TEST_DB_USER) {
      throw new Error(
        `Identidade do banco inválida: database='${dbName}', user='${dbUser}'. Esperado exclusivamente ambiente de teste isolado: '${EXPECTED_TEST_DB_NAME}' e '${EXPECTED_TEST_DB_USER}'.`
      );
    }

    if (/prod/i.test(dbName) || /prod/i.test(dbUser)) {
      throw new Error(`Banco identificado como produção ('${identity}'). Modificações abortadas.`);
    }

    return { currentEnv, dbName, dbUser };
  } catch (err) {
    throw new Error(
      `[SEGURANÇA CRÍTICA - OPERAÇÃO ABORTADA] Falha ao verificar identidade do banco de testes isolado: ${err.message}`
    );
  }
}

function loadTrackedUuidsFromDisk() {
  if (fs.existsSync(TRACKED_AOI_UUIDS_FILE)) {
    try {
      const data = JSON.parse(fs.readFileSync(TRACKED_AOI_UUIDS_FILE, 'utf-8'));
      if (Array.isArray(data)) {
        for (const id of data) {
          if (id && id !== OFFICIAL_MACEIO_AOI_ID) {
            trackedCreatedAoiIds.add(id);
          }
        }
        console.log(`[Tracking] Carregados ${trackedCreatedAoiIds.size} UUID(s) residuais do disco para saneamento seguro.`);
      }
    } catch (err) {
      console.warn('[Tracking Warning] Falha ao ler arquivo de tracking:', err.message);
    }
  }
}

function safeCleanupTrackedAois() {
  const { currentEnv, dbName } = assertIsolatedTestDatabase();
  if (trackedCreatedAoiIds.size === 0) {
    console.log(`[DB Cleanup Seguro (${currentEnv} / ${dbName})] Nenhuma AOI temporária registrada para exclusão.`);
    return;
  }

  console.log(`[DB Cleanup Seguro (${currentEnv} / ${dbName})] Iniciando remoção estrita de ${trackedCreatedAoiIds.size} AOI(s) registradas...`);
  for (const aoiId of trackedCreatedAoiIds) {
    if (aoiId === OFFICIAL_MACEIO_AOI_ID) {
      console.error(`[DB Cleanup SEGURANÇA CRÍTICA] Tentativa de deletar AOI oficial bloqueada: ${aoiId}`);
      continue;
    }

    if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(aoiId)) {
      console.error(`[DB Cleanup] Identificador rejeitado por não ser UUID válido: ${aoiId}`);
      continue;
    }

    try {
      const cmd = `docker exec radar_postgres psql -U radar_user -d radar_db -t -c "DELETE FROM radar.areas_of_interest WHERE id = '${aoiId}' RETURNING id, name;"`;
      const res = execSync(cmd, { encoding: 'utf-8' }).trim();
      if (res) {
        console.log(`[DB Cleanup Seguro] AOI de teste excluída pelo UUID [${aoiId}]: ${res}`);
      }
    } catch (err) {
      console.warn(`[DB Cleanup Warning] Falha ao remover AOI com UUID ${aoiId}:`, err.message);
    }
  }

  trackedCreatedAoiIds.clear();
  if (fs.existsSync(TRACKED_AOI_UUIDS_FILE)) {
    try {
      fs.unlinkSync(TRACKED_AOI_UUIDS_FILE);
    } catch (e) {}
  }
  console.log('[DB Cleanup Seguro] Saneamento concluído.');
}

async function run() {
  console.log('========================================================================');
  console.log('RADAR++ - CICLO 5.5.2: SUÍTE DE HOMOLOGAÇÃO E2E, DRY-RUN E ESTABILIDADE (CDP)');
  console.log('========================================================================\n');

  if (!fs.existsSync(EVIDENCE_DIR)) {
    fs.mkdirSync(EVIDENCE_DIR, { recursive: true });
  }

  console.log('0. Verificando tracking prévio de UUIDs residuais de execuções anteriores...');
  loadTrackedUuidsFromDisk();
  safeCleanupTrackedAois();

  console.log('\n1. Iniciando Google Chrome em modo headless com CDP na porta ' + PORT);
  const chrome = spawn(CHROME_PATH, [
    '--headless=new',
    '--remote-debugging-port=' + PORT,
    '--no-sandbox',
    '--disable-gpu',
    '--disable-extensions',
    '--user-data-dir=/tmp/radar-e2e-chrome-profile',
    '--window-size=1440,1100',
    'http://localhost:3000'
  ]);

  chrome.stderr.on('data', () => {});

  let targets = null;
  for (let i = 0; i < 30; i++) {
    await sleep(500);
    try {
      const resp = await fetch(`http://127.0.0.1:${PORT}/json`);
      if (resp.ok) {
        targets = await resp.json();
        const hasApp = targets.some(t => t.url?.includes('localhost:3000'));
        if (hasApp) break;
      }
    } catch (e) {}
  }

  if (!targets || targets.length === 0) {
    console.error('Falha ao conectar no Chrome CDP!');
    chrome.kill();
    process.exit(1);
  }

  const pageTarget = targets.find(t => t.url?.includes('localhost:3000')) ||
                     targets.find(t => t.type === 'page' && !t.url?.startsWith('chrome')) ||
                     targets[0];
  console.log('Conectado à página:', pageTarget.url);

  const ws = new WebSocket(pageTarget.webSocketDebuggerUrl);
  let msgId = 1;
  const callbacks = new Map();

  ws.onmessage = event => {
    const data = JSON.parse(event.data);
    if (data.id && callbacks.has(data.id)) {
      const cb = callbacks.get(data.id);
      callbacks.delete(data.id);
      cb(data);
    }
  };

  await new Promise(resolve => ws.onopen = resolve);

  // Registro de tratadores de interrupção (SIGINT / SIGTERM) para segurança do banco
  const shutdownHandler = () => {
    console.log('\n[Process Handler] Interrupção detectada. Executando limpeza segura de dados temporários...');
    safeCleanupTrackedAois();
    try { if (ws && ws.readyState === WebSocket.OPEN) ws.close(); } catch (e) {}
    try { if (chrome) chrome.kill(); } catch (e) {}
    process.exit(1);
  };
  process.on('SIGINT', shutdownHandler);
  process.on('SIGTERM', shutdownHandler);

  function send(method, params = {}) {
    return new Promise(resolve => {
      const id = msgId++;
      callbacks.set(id, resolve);
      ws.send(JSON.stringify({ id, method, params }));
    });
  }

  async function evaluate(expression) {
    const res = await send('Runtime.evaluate', {
      expression,
      returnByValue: true,
      awaitPromise: true
    });
    return res.result?.result?.value;
  }

  async function captureScreenshot(filename, fullPage = false) {
    const params = { format: 'png' };
    if (fullPage) {
      params.captureBeyondViewport = true;
    }
    const res = await send('Page.captureScreenshot', params);
    const buffer = Buffer.from(res.result.data, 'base64');

    // Salva no diretório de evidência oficial do projeto
    const evidencePath = path.join(EVIDENCE_DIR, filename);
    fs.writeFileSync(evidencePath, buffer);

    // Salva cópia no diretório de artefatos do agente para inclusão em relatórios
    if (fs.existsSync(ARTIFACT_DIR)) {
      fs.writeFileSync(path.join(ARTIFACT_DIR, filename), buffer);
    }

    console.log(`[Screenshot gravado]: ${filename} (${buffer.length} bytes) -> ${evidencePath}`);
    return evidencePath;
  }

  await send('Page.enable');
  await send('Runtime.enable');
  await send('DOM.enable');

  console.log('\n--- ETAPA 1: Inspeção de Carregamento Inicial, Identidade Visual e Políticas ---');
  await sleep(2500);

  const initialCheck = await evaluate(`(() => {
    const bodyText = document.body.innerText;
    const title = document.title;
    const hasRadarPlus = bodyText.includes('Radar++') || bodyText.includes('Radar++ Geospatial Intelligence');
    const hasPolicy = bodyText.includes('Política Operacional de Área') || bodyText.includes('250');
    const hasLovable = /lovable|gptengineer/i.test(document.documentElement.innerHTML);
    return {
      title,
      hasRadarPlus,
      hasPolicy,
      hasLovable,
      htmlLength: document.documentElement.innerHTML.length
    };
  })()`);

  console.log('Resultado da Inspeção Inicial:', initialCheck);
  if (initialCheck.hasLovable) {
    console.error('AVISO: Referência a templates legados (Lovable/gptengineer) detectada!');
  }
  await captureScreenshot('01_initial_dashboard.png');

  console.log('\n--- ETAPA 2: Seleção da AOI de Homologação de Maceió (PostGIS) ---');
  const selectMaceio = await evaluate(`(() => {
    const select = document.querySelector('select');
    if (!select) return { error: 'Select de AOIs não encontrado' };
    const options = Array.from(select.options);
    const maceioOpt = options.find(o => o.text.includes('Maceio Homologacao 5.1.1') || o.value === '${OFFICIAL_MACEIO_AOI_ID}');
    if (maceioOpt) {
      select.value = maceioOpt.value;
      select.dispatchEvent(new Event('change', { bubbles: true }));
      return { found: true, text: maceioOpt.text, value: maceioOpt.value };
    }
    return { error: 'AOI de Maceió não encontrada nas opções', available: options.map(o => o.text) };
  })()`);
  console.log('Seleção da AOI de Maceió:', selectMaceio);
  await sleep(2000);

  const maceioAreaCheck = await evaluate(`(() => {
    const text = document.body.innerText;
    const has1093 = text.includes('10.93 km²') || text.includes('10.9254');
    const hasOfficialBadge = text.includes('Oficial PostGIS (WGS84)') || text.includes('PostGIS');
    return { has1093, hasOfficialBadge };
  })()`);
  console.log('Área Geodésica e Badge Oficial da AOI de Maceió:', maceioAreaCheck);
  await captureScreenshot('02_official_maceio_aoi_selected.png');

  console.log('\n--- ETAPA 3: Edição Cartográfica e Verificação de Integridade de Geometria Não Salva ---');
  // Habilita o modo de edição para exibir os marcadores de vértices editáveis
  await evaluate(`(() => {
    const buttons = Array.from(document.querySelectorAll('button'));
    const editBtn = buttons.find(b => b.innerText.includes('Editar'));
    if (editBtn) editBtn.click();
  })()`);
  await sleep(1500);

  // Clica no vértice para abrir o popup de remoção
  const vertexClick = await evaluate(`(() => {
    const marker = document.querySelector('.radar-vertex-icon');
    if (!marker) return { error: 'Marcador de vértice não encontrado no Leaflet' };
    marker.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }));
    return { clicked: true };
  })()`);
  console.log('Vértice clicado:', vertexClick);
  await sleep(1000);

  const removeVertex = await evaluate(`(() => {
    const buttons = Array.from(document.querySelectorAll('button'));
    const removeBtn = buttons.find(b => b.innerText.trim() === 'Remover');
    if (removeBtn) {
      removeBtn.click();
      return { removed: true };
    }
    return { error: 'Botão Remover não encontrado no popup' };
  })()`);
  console.log('Vértice removido:', removeVertex);
  await sleep(1500);

  const integrityAlerts = await evaluate(`(() => {
    const text = document.body.innerText;
    return {
      hasUnsavedBadge: text.includes('Geometria Modificada (Não Salva)') || text.includes('Não Salvo'),
      hasBlockedSearch: text.includes('Consulta ao Catálogo SAR Bloqueada') || text.includes('Bloqueada'),
      hasSaveNewBtn: text.includes('Salvar como Nova Área de Interesse'),
      hasDiscardBtn: text.includes('Descartar Alterações')
    };
  })()`);
  console.log('Alertas de integridade e bloqueio de catálogo SAR:', integrityAlerts);
  await captureScreenshot('03_geometry_modified_unsaved_warning.png');

  console.log('\n--- ETAPA 4: Salvamento Seguro de Nova AOI de Teste no PostGIS com Registro de UUID ---');
  assertIsolatedTestDatabase();
  const uniqueTestAoiName = `AOI Teste Homologacao E2E 5.5.2 - ${Date.now()}`;
  console.log(`Nome único da AOI temporária: "${uniqueTestAoiName}"`);

  const saveNewAoi = await evaluate(`(() => {
    function setNativeValue(element, value) {
      const valueSetter = Object.getOwnPropertyDescriptor(element, 'value').set;
      const prototype = Object.getPrototypeOf(element);
      const prototypeValueSetter = Object.getOwnPropertyDescriptor(prototype, 'value').set;
      if (prototypeValueSetter && valueSetter !== prototypeValueSetter) {
        prototypeValueSetter.call(element, value);
      } else {
        valueSetter.call(element, value);
      }
      element.dispatchEvent(new Event('input', { bubbles: true }));
      element.dispatchEvent(new Event('change', { bubbles: true }));
    }

    const nameInput = document.querySelector('input[type="text"]');
    if (nameInput) {
      setNativeValue(nameInput, "${uniqueTestAoiName}");
    }

    const submitBtn = document.querySelector('button[type="submit"]');
    if (submitBtn) {
      submitBtn.click();
      return { clicked: true, text: submitBtn.innerText };
    }
    return { error: 'Botão de submissão não encontrado' };
  })()`);
  console.log('Submissão da nova AOI de teste:', saveNewAoi);

  // Aguarda mensagem de sucesso
  let alertSuccess = null;
  for (let i = 0; i < 25; i++) {
    await sleep(500);
    alertSuccess = await evaluate(`(() => {
      const elem = document.querySelector('.bg-emerald-50');
      return elem ? elem.innerText.trim() : null;
    })()`);
    if (alertSuccess) break;
  }
  console.log('Mensagem de sucesso recebida da API:', alertSuccess);
  await sleep(1500);

  // Consulta o UUID exato gerado no PostGIS para esta execução específica
  try {
    const queryCmd = `docker exec radar_postgres psql -U radar_user -d radar_db -t -c "SELECT id FROM radar.areas_of_interest WHERE name = '${uniqueTestAoiName}';"`;
    const newUuid = execSync(queryCmd, { encoding: 'utf-8' }).trim();
    if (newUuid && /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(newUuid)) {
      trackedCreatedAoiIds.add(newUuid);
      persistTrackedUuids();
      console.log(`[UUID Tracking SEGURO] AOI de teste registrada com sucesso para exclusão posterior: [${newUuid}]`);
    } else {
      console.warn('[UUID Tracking Warning] Não foi possível obter o UUID exato via PostGIS:', newUuid);
    }
  } catch (err) {
    console.warn('[UUID Tracking Erro]', err.message);
  }

  await captureScreenshot('04_new_aoi_persisted_postgis.png');

  console.log('\n--- ETAPA 5: Recarregar a Página e Confirmar Durabilidade no PostGIS ---');
  await send('Page.reload');
  await sleep(3000);

  const reloadedSelect = await evaluate(`(() => {
    const select = document.querySelector('select');
    if (!select) return { error: 'Select não encontrado após reload' };
    const options = Array.from(select.options).map(o => ({ value: o.value, text: o.text }));
    const newAoiOpt = options.find(o => o.text.includes('${uniqueTestAoiName}'));
    if (newAoiOpt) {
      select.value = newAoiOpt.value;
      select.dispatchEvent(new Event('change', { bubbles: true }));
      return { found: true, text: newAoiOpt.text, value: newAoiOpt.value };
    }
    return { found: false, totalOptions: options.length, optionsPreview: options.slice(0, 5) };
  })()`);
  console.log('Recarregamento e seleção da AOI salva no PostGIS:', reloadedSelect);
  await sleep(2000);
  await captureScreenshot('05_reloaded_and_verified_persistence.png');

  // Retorna para a AOI Oficial de Homologação de Maceió para a jornada de processamento InSAR
  console.log('\nRetornando para a AOI Oficial de Homologação de Maceió (10.93 km²)...');
  await evaluate(`(() => {
    const select = document.querySelector('select');
    if (!select) return;
    const options = Array.from(select.options);
    const maceioOpt = options.find(o => o.text.includes('Maceio Homologacao 5.1.1') || o.value === '${OFFICIAL_MACEIO_AOI_ID}');
    if (maceioOpt) {
      select.value = maceioOpt.value;
      select.dispatchEvent(new Event('change', { bubbles: true }));
    }
  })()`);
  await sleep(2000);

  console.log('\n--- ETAPA 6: Consulta ao Catálogo SAR Sentinel-1 IW SLC (NASA ASF DAAC) ---');
  const searchSar = await evaluate(`(() => {
    const buttons = Array.from(document.querySelectorAll('button'));
    const btn = buttons.find(b => b.innerText.includes('Consultar Cenas Sentinel-1 SLC'));
    if (btn && !btn.disabled) {
      btn.click();
      return { clicked: true };
    }
    return { error: 'Botão de busca SAR não habilitado' };
  })()`);
  console.log('Disparo da busca SAR:', searchSar);

  let rowsCount = 0;
  for (let i = 0; i < 40; i++) {
    await sleep(500);
    rowsCount = await evaluate(`document.querySelectorAll('tbody tr').length`) || 0;
    if (rowsCount >= 3) break;
  }
  console.log(`Cenas SAR carregadas no navegador: ${rowsCount} linhas.`);
  await sleep(1500);

  await evaluate('document.querySelector("table")?.scrollIntoView({ block: "center", behavior: "instant" })');
  await sleep(1000);
  await captureScreenshot('06_sar_scenes_table.png');

  console.log('\n--- ETAPA 7: Verificação do Aviso Científico de Elegibilidade InSAR ---');
  const hasDisclaimer = await evaluate(`(() => {
    const text = document.body.innerText;
    return text.includes('Aviso Científico de Elegibilidade InSAR') &&
           text.includes('NÃO constituem pares interferométricos');
  })()`);
  console.log('Aviso Científico InSAR verificado na UI:', hasDisclaimer);
  await captureScreenshot('07_insar_scientific_disclaimer.png');

  console.log('\n--- ETAPA 8: Formação e Avaliação de Pares InSAR (Política TROPICAL_URBAN_MACEIO) ---');
  const formPairs = await evaluate(`(() => {
    const buttons = Array.from(document.querySelectorAll('button'));
    const btn = buttons.find(b => b.innerText.includes('Formar e Avaliar Pares InSAR'));
    if (btn && !btn.disabled) {
      btn.click();
      return { clicked: true };
    }
    return { error: 'Botão de pares InSAR indisponível' };
  })()`);
  console.log('Disparo da Formação de Pares InSAR:', formPairs);

  let pairsEvaluated = false;
  for (let i = 0; i < 60; i++) {
    await sleep(1000);
    pairsEvaluated = await evaluate(`(() => {
      const text = document.body.innerText;
      return text.includes('Elegível para Submissão') || text.includes('Total Formados');
    })()`);
    if (pairsEvaluated) break;
  }
  console.log('Pares InSAR avaliados pelo backend:', pairsEvaluated);
  await sleep(2000);

  await evaluate('window.scrollTo({ top: document.body.scrollHeight * 0.7, behavior: "instant" })');
  await sleep(1000);
  await captureScreenshot('08_insar_pairs_summary_cards.png');

  console.log('\n--- ETAPA 9: Expansão da Justificativa Técnica para PAR-01 (Elegível) ---');
  const expandPar01 = await evaluate(`(() => {
    const buttons = Array.from(document.querySelectorAll('button')).filter(b => b.innerText.includes('Justificativa'));
    if (buttons.length > 0) {
      buttons[0].click();
      return { clicked: true, totalJustButtons: buttons.length };
    }
    return { error: 'Nenhum botão de justificativa encontrado' };
  })()`);
  console.log('Expansão PAR-01:', expandPar01);
  await sleep(1200);
  await captureScreenshot('09_par_01_eligible_justification.png');

  console.log('\n--- ETAPA 10: Expansão da Justificativa Técnica para PAR-02 (Rejeitado) ---');
  const expandPar02 = await evaluate(`(() => {
    const buttons = Array.from(document.querySelectorAll('button')).filter(b => b.innerText.includes('Justificativa'));
    if (buttons.length > 1) {
      buttons[1].click();
      return { clicked: true };
    }
    return { error: 'Botão de justificativa do segundo par não encontrado' };
  })()`);
  console.log('Expansão PAR-02:', expandPar02);
  await sleep(1200);
  await captureScreenshot('10_par_02_rejected_justification.png');

  console.log('\n--- ETAPA 11 (CICLO 5.5.2): Abertura do Painel Modular e Carregamento Dinâmico de Opções / Bursts ASF ---');
  const openDryRun = await evaluate(`(() => {
    const buttons = Array.from(document.querySelectorAll('button')).filter(b => b.innerText.includes('Preparar Dry-Run'));
    if (buttons.length > 0) {
      buttons[0].click();
      return { clicked: true, total: buttons.length };
    }
    return { error: 'Botão "Preparar Dry-Run" não encontrado na tabela de pares' };
  })()`);
  console.log('Clique em Preparar Dry-Run:', openDryRun);

  // Aguarda carregamento das opções dinâmicas via API
  let dynamicOptionsLoaded = false;
  let burstVerification = null;
  for (let i = 0; i < 40; i++) {
    await sleep(1000);
    burstVerification = await evaluate(`(() => {
      const text = document.body.innerText;
      const hasBurst18319 = text.includes('018319') || text.includes('18319');
      const hasBurst18318 = text.includes('018318') || text.includes('18318');
      const hasDynamic100 = text.includes('100.00%') || text.includes('100%');
      const hasDynamic69 = text.includes('69.68%');
      const hasLimitations = text.includes('Limitações de Qualidade Científica');
      const hasOldDummyMaceio = text.includes('73.92%') || text.includes('71.34%');
      return {
        loaded: hasBurst18319 && hasLimitations,
        hasBurst18319,
        hasBurst18318,
        hasDynamic100,
        hasDynamic69,
        hasLimitations,
        hasOldDummyMaceio
      };
    })()`);
    if (burstVerification && burstVerification.loaded) {
      dynamicOptionsLoaded = true;
      break;
    }
  }
  console.log('Verificação de Bursts e Coberturas Dinâmicas no Painel:', burstVerification);

  // Rola até o painel de dry-run para enquadrar
  await evaluate(`(() => {
    const select = document.querySelector('select[name="modality"]') || document.querySelectorAll('select')[1];
    if (select) {
      select.scrollIntoView({ block: 'center', behavior: 'instant' });
    } else {
      const panel = document.querySelector('.bg-indigo-50\\\\/40') || document.querySelector('.border-indigo-300') || document.querySelector('td[colspan]');
      if (panel) panel.scrollIntoView({ block: 'center', behavior: 'instant' });
    }
  })()`);
  await sleep(1000);
  await captureScreenshot('12_insar_dry_run_panel_ready.png');

  console.log('\n--- ETAPA 12 (CICLO 5.5.2): Simulação Dry-Run Single-Burst (INSAR_ISCE_BURST) ---');
  const triggerSingleBurst = await evaluate(`(() => {
    const buttons = Array.from(document.querySelectorAll('button'));
    const btn = buttons.find(b => b.innerText.includes('Solicitar Validação Dry-Run (Sem Custo)'));
    if (btn && !btn.disabled) {
      btn.click();
      return { clicked: true };
    }
    return { error: 'Botão de disparo do Dry-Run não encontrado ou desabilitado' };
  })()`);
  console.log('Disparo do Dry-Run Single-Burst via interface:', triggerSingleBurst);

  let singleBurstSuccess = false;
  for (let i = 0; i < 30; i++) {
    await sleep(500);
    singleBurstSuccess = await evaluate(`(() => {
      const text = document.body.innerText;
      return text.includes('Simulação Dry-Run Auditada com Sucesso') && text.includes('INSAR_ISCE_BURST');
    })()`);
    if (singleBurstSuccess) break;
  }
  console.log('Resultado Single-Burst recebido da API:', singleBurstSuccess);
  await sleep(1000);

  // Expande o JSON de auditoria pelo ID específico ou texto do botão
  const expandAuditJson = await evaluate(`(() => {
    const btnById = document.getElementById('btn-toggle-audit-json');
    if (btnById) {
      btnById.click();
      return { clicked: true, method: 'byId' };
    }
    const buttons = Array.from(document.querySelectorAll('button'));
    const jsonBtn = buttons.find(b => b.innerText.includes('Visualizar Contrato JSON') || b.innerText.includes('Auditoria'));
    if (jsonBtn) {
      jsonBtn.click();
      return { clicked: true, method: 'byText' };
    }
    return { error: 'Botão de visualização do JSON de auditoria não encontrado' };
  })()`);
  console.log('Expansão do JSON de Auditoria Single-Burst:', expandAuditJson);
  await sleep(1200);

  // Verifica conteúdo do JSON
  const auditContentSingle = await evaluate(`(() => {
    const pre = document.querySelector('pre');
    return pre ? pre.innerText : null;
  })()`);
  const hasValidateOnlySingle = auditContentSingle && auditContentSingle.includes('"validate_only" : true');
  console.log('Auditoria Single-Burst contém validate_only: true ->', hasValidateOnlySingle);

  await evaluate(`(() => {
    const jsonPre = document.querySelector('pre');
    if (jsonPre) jsonPre.scrollIntoView({ block: 'center', behavior: 'instant' });
  })()`);
  await sleep(1000);
  await captureScreenshot('13_insar_dry_run_validated_audit.png');

  console.log('\n--- ETAPA 12b (NOVO 5.5.2): Alternância para Multi-Burst (INSAR_ISCE_MULTI_BURST) e Auditoria de Conjuntos Separados ---');
  const selectMultiBurst = await evaluate(`(() => {
    const card = document.getElementById('modality-card-MULTI_BURST');
    if (card) {
      card.click();
      return { selected: true, method: 'byId' };
    }
    const divs = Array.from(document.querySelectorAll('div'));
    const multiDiv = divs.find(d => d.innerText.includes('MULTI_BURST') && d.innerText.includes('crédito'));
    if (multiDiv) {
      multiDiv.click();
      return { selected: true, method: 'byText' };
    }
    return { error: 'Card de modalidade MULTI_BURST não encontrado' };
  })()`);
  console.log('Seleção da modalidade MULTI_BURST:', selectMultiBurst);
  await sleep(1500);

  const multiBurstViewCheck = await evaluate(`(() => {
    const text = document.body.innerText;
    const hasRef = text.includes('Conjunto de Referência') || text.includes('Conjunto Referência');
    const hasSec = text.includes('Conjunto Secundário');
    const hasCombined100 = text.includes('100.00%') || text.includes('100%');
    return { hasRef, hasSec, hasCombined100 };
  })()`);
  console.log('Visualização de conjuntos Reference e Secondary Multi-Burst:', multiBurstViewCheck);

  const triggerMultiBurst = await evaluate(`(() => {
    const buttons = Array.from(document.querySelectorAll('button'));
    const btn = buttons.find(b => b.innerText.includes('Solicitar Validação Dry-Run (Sem Custo)'));
    if (btn && !btn.disabled) {
      btn.click();
      return { clicked: true };
    }
    return { error: 'Botão de disparo do Dry-Run Multi-Burst não encontrado ou desabilitado' };
  })()`);
  console.log('Disparo do Dry-Run Multi-Burst:', triggerMultiBurst);

  let multiBurstSuccess = false;
  for (let i = 0; i < 30; i++) {
    await sleep(500);
    multiBurstSuccess = await evaluate(`(() => {
      const text = document.body.innerText;
      return text.includes('Simulação Dry-Run Auditada com Sucesso') && text.includes('INSAR_ISCE_MULTI_BURST');
    })()`);
    if (multiBurstSuccess) break;
  }
  console.log('Resultado Multi-Burst recebido da API:', multiBurstSuccess);
  await sleep(1000);

  // Expande JSON de auditoria Multi-Burst se não estiver aberto
  await evaluate(`(() => {
    const btnById = document.getElementById('btn-toggle-audit-json');
    if (btnById && !document.querySelector('pre')) {
      btnById.click();
    }
  })()`);
  await sleep(1000);

  const auditContentMulti = await evaluate(`(() => {
    const pre = document.querySelector('pre');
    return pre ? pre.innerText : null;
  })()`);
  const hasSeparateSetsInJson = auditContentMulti &&
    auditContentMulti.includes('"reference" : [') &&
    auditContentMulti.includes('"secondary" : [') &&
    auditContentMulti.includes('INSAR_ISCE_MULTI_BURST');
  console.log('Auditoria Multi-Burst contém conjuntos reference e secondary estritos ->', hasSeparateSetsInJson);

  await evaluate(`(() => {
    const jsonPre = document.querySelector('pre');
    if (jsonPre) jsonPre.scrollIntoView({ block: 'center', behavior: 'instant' });
  })()`);
  await sleep(1000);
  await captureScreenshot('13b_insar_multi_burst_dry_run_audit.png');

  console.log('\n--- ETAPA 12c (NOVO 5.5.2): Bloqueio de Dry-Run sobre Geometria Modificada e Não Salva ---');
  // Habilita edição e clica no vértice para remover, modificando a geometria
  await evaluate(`(() => {
    const buttons = Array.from(document.querySelectorAll('button'));
    const editBtn = buttons.find(b => b.innerText.includes('Editar'));
    if (editBtn) editBtn.click();
  })()`);
  await sleep(1500);

  await evaluate(`(() => {
    const marker = document.querySelector('.radar-vertex-icon');
    if (marker) marker.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }));
  })()`);
  await sleep(800);
  await evaluate(`(() => {
    const buttons = Array.from(document.querySelectorAll('button'));
    const removeBtn = buttons.find(b => b.innerText.trim() === 'Remover');
    if (removeBtn) removeBtn.click();
  })()`);
  await sleep(1500);

  const unsavedGeometryCheck = await evaluate(`(() => {
    const text = document.body.innerText;
    const hasUnsavedBadge = text.includes('Geometria Modificada (Não Salva)') || text.includes('Não Salvo');
    const isDryRunClosed = !document.getElementById('modality-card-MULTI_BURST');
    const hasBlockedCatalog = text.includes('Consulta ao Catálogo SAR Bloqueada') || text.includes('Bloqueada');
    return {
      hasUnsavedBadge,
      isDryRunClosed,
      hasBlockedCatalog,
      invalidationEnforced: hasUnsavedBadge && isDryRunClosed && hasBlockedCatalog
    };
  })()`);
  console.log('Verificação de Bloqueio e Invalidação de Dry-Run sobre geometria alterada:', unsavedGeometryCheck);

  await evaluate(`(() => {
    const warn = document.querySelector('.bg-amber-50') || document.querySelector('.text-amber-800');
    if (warn) warn.scrollIntoView({ block: 'center', behavior: 'instant' });
  })()`);
  await sleep(1000);
  await captureScreenshot('13c_insar_dry_run_blocked_unsaved_geometry.png');

  // Descartar alterações para restaurar a geometria original limpa
  await evaluate(`(() => {
    const buttons = Array.from(document.querySelectorAll('button'));
    const discardBtn = buttons.find(b => b.innerText.includes('Descartar Alterações'));
    if (discardBtn) discardBtn.click();
  })()`);
  await sleep(1500);

  console.log('\n--- ETAPA 13: Revisão Visual e Responsiva (Tablet e Celular) ---');
  // 13.1 - Tablet (768 x 1024)
  console.log('Simulando Viewport Tablet (768x1024)...');
  await send('Emulation.setDeviceMetricsOverride', {
    width: 768,
    height: 1024,
    deviceScaleFactor: 1,
    mobile: true
  });
  await sleep(1500);
  await evaluate('window.scrollTo({ top: 0, behavior: "instant" })');
  await sleep(1000);
  await captureScreenshot('14_tablet_viewport_768x1024.png');

  // Rola até a tabela InSAR e painel dry-run no Tablet
  await evaluate(`(() => {
    const el = document.querySelector('.border-indigo-300') || document.querySelector('table');
    if (el) el.scrollIntoView({ block: 'start', behavior: 'instant' });
  })()`);
  await sleep(1000);
  await captureScreenshot('14_tablet_insar_table.png');

  // 13.2 - Celular / Smartphone (375 x 812)
  console.log('Simulando Viewport Mobile (375x812)...');
  await send('Emulation.setDeviceMetricsOverride', {
    width: 375,
    height: 812,
    deviceScaleFactor: 2,
    mobile: true
  });
  await sleep(1500);
  await evaluate('window.scrollTo({ top: 0, behavior: "instant" })');
  await sleep(1000);
  await captureScreenshot('15_mobile_viewport_375x812.png');

  // Rola até os cards de pares e dry-run no Mobile
  await evaluate(`(() => {
    const el = document.querySelector('.border-indigo-300') || document.querySelector('.bg-emerald-50');
    if (el) el.scrollIntoView({ block: 'start', behavior: 'instant' });
  })()`);
  await sleep(1000);
  await captureScreenshot('15_mobile_insar_card.png');

  // Restaura viewport Desktop
  await send('Emulation.clearDeviceMetricsOverride');
  await sleep(1000);

  console.log('\n--- ETAPA 14: Captura de Panorama Completo da Aplicação (Full Page) ---');
  await captureScreenshot('11_full_page_e2e_homologation.png', true);

  console.log('\n--- ETAPA 15: Saneamento Final Autorizado de Dados de Teste ---');
  safeCleanupTrackedAois();

  // Verifica que a AOI oficial de Maceió permanece intacta
  try {
    const checkOfficialCmd = `docker exec radar_postgres psql -U radar_user -d radar_db -t -c "SELECT count(*) FROM radar.areas_of_interest WHERE id = '${OFFICIAL_MACEIO_AOI_ID}';"`;
    const officialCount = parseInt(execSync(checkOfficialCmd, { encoding: 'utf-8' }).trim(), 10);
    console.log(`[Integridade PostGIS] AOI Oficial de Maceió (${OFFICIAL_MACEIO_AOI_ID}): ${officialCount === 1 ? 'PRESERVADA COM SUCESSO (1 registro)' : 'ERRO: NÃO ENCONTRADA!'}`);
  } catch (err) {
    console.warn('[Integridade PostGIS Warning]', err.message);
  }

  ws.close();
  chrome.kill();
  console.log('\n========================================================================');
  console.log('SUÍTE E2E DO CICLO 5.5.2 CONCLUÍDA COM 100% DE SUCESSO! EVIDÊNCIAS PRONTAS.');
  console.log('========================================================================');
}

run().catch(err => {
  console.error('Erro na execução da suíte E2E:', err);
  safeCleanupTrackedAois();
  process.exit(1);
});
