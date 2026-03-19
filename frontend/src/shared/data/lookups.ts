import type { SearchOption } from "../components/SearchSelect";

export const COMPANY_OPTIONS: SearchOption[] = [
  {
    value: "C001",
    label: "Modular Manufacturing Korea",
    description: "서울 본사 · 재무/구매 통합 운영",
    meta: "Seoul HQ",
    keywords: ["한국", "manufacturing", "hq"],
  },
  {
    value: "C002",
    label: "Modular Components Vietnam",
    description: "호치민 조달 허브 · 글로벌 소싱 센터",
    meta: "APAC Hub",
    keywords: ["베트남", "sourcing", "apac"],
  },
];

export const PLANT_OPTIONS: SearchOption[] = [
  {
    value: "P001",
    label: "평택 메인 플랜트",
    description: "C001 · 조립 및 완제품 생산",
    meta: "C001",
    keywords: ["평택", "assembly", "main", "C001"],
  },
  {
    value: "P002",
    label: "구미 전자부품 플랜트",
    description: "C001 · 반제품 및 전장 모듈",
    meta: "C001",
    keywords: ["구미", "component", "C001"],
  },
  {
    value: "P101",
    label: "빈증 소싱 플랜트",
    description: "C002 · 수입 부자재 집하 및 분배",
    meta: "C002",
    keywords: ["vietnam", "distribution", "C002"],
  },
];

export const VENDOR_OPTIONS: SearchOption[] = [
  {
    value: "V1001",
    label: "세영소재",
    description: "전기차 배터리 케이스 및 알루미늄 가공",
    meta: "Preferred",
    keywords: ["battery", "aluminum", "국내"],
  },
  {
    value: "V1002",
    label: "Nova Circuit",
    description: "PCB 및 전장 모듈 전문 공급업체",
    meta: "Strategic",
    keywords: ["pcb", "module", "electronics"],
  },
  {
    value: "V1003",
    label: "Pacific Industrial Trading",
    description: "해외 범용 자재 및 MRO 소싱",
    meta: "Global",
    keywords: ["mro", "trading", "import"],
  },
];

export const CURRENCY_OPTIONS: SearchOption[] = [
  { value: "KRW", label: "KRW", description: "대한민국 원", meta: "Local" },
  { value: "USD", label: "USD", description: "US Dollar", meta: "Global" },
  { value: "JPY", label: "JPY", description: "Japanese Yen", meta: "APAC" },
  { value: "EUR", label: "EUR", description: "Euro", meta: "EMEA" },
];

export const PAYMENT_TERM_OPTIONS: SearchOption[] = [
  { value: "NET30", label: "NET30", description: "세금계산서 발행 후 30일", meta: "Standard" },
  { value: "NET45", label: "NET45", description: "검수 완료 후 45일", meta: "Long" },
  { value: "PREPAID", label: "PREPAID", description: "선지급 조건", meta: "Advance" },
  { value: "EOM+30", label: "EOM+30", description: "월말 기준 30일", meta: "Month-end" },
];

export const UNIT_OPTIONS: SearchOption[] = [
  { value: "EA", label: "EA", description: "Each", meta: "Count" },
  { value: "KG", label: "KG", description: "Kilogram", meta: "Weight" },
  { value: "L", label: "L", description: "Liter", meta: "Volume" },
  { value: "M", label: "M", description: "Meter", meta: "Length" },
  { value: "M2", label: "M2", description: "Square Meter", meta: "Area" },
  { value: "M3", label: "M3", description: "Cubic Meter", meta: "Volume" },
  { value: "SET", label: "SET", description: "Set", meta: "Bundle" },
  { value: "BOX", label: "BOX", description: "Box", meta: "Package" },
  { value: "ROLL", label: "ROLL", description: "Roll", meta: "Package" },
];

export const ITEM_GROUP_OPTIONS: SearchOption[] = [
  { value: "RAW_METAL", label: "원재료 / 금속", description: "판재, 봉재, 가공소재", meta: "Material" },
  { value: "ELEC_PART", label: "전장 부품", description: "PCB, 하네스, 센서류", meta: "Electrical" },
  { value: "PACKING", label: "포장 자재", description: "박스, 라벨, 보호재", meta: "Packaging" },
  { value: "MRO", label: "MRO", description: "소모품, 공구, 유지보수 자재", meta: "Operations" },
];

export const LOCALE_OPTIONS: SearchOption[] = [
  { value: "ko", label: "Korean", description: "한국어", meta: "KR" },
  { value: "en", label: "English", description: "영어", meta: "EN" },
  { value: "ja", label: "Japanese", description: "일본어", meta: "JA" },
  { value: "zh", label: "Chinese", description: "중국어", meta: "ZH" },
];
