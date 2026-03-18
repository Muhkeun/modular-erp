import { useNavigate, useParams } from "react-router-dom";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useState, useEffect } from "react";
import { Save, ArrowLeft, Trash2 } from "lucide-react";
import PageHeader from "../../../shared/components/PageHeader";
import api from "../../../shared/api/client";

interface ItemForm {
  code: string;
  itemType: string;
  itemGroup: string;
  unitOfMeasure: string;
  specification: string;
  weight: string;
  volume: string;
  makerName: string;
  makerItemNo: string;
  qualityInspectionRequired: boolean;
  phantomBom: boolean;
  translations: { locale: string; name: string; description: string }[];
}

const defaultForm: ItemForm = {
  code: "", itemType: "MATERIAL", itemGroup: "", unitOfMeasure: "EA",
  specification: "", weight: "", volume: "", makerName: "", makerItemNo: "",
  qualityInspectionRequired: false, phantomBom: false,
  translations: [{ locale: "ko", name: "", description: "" }],
};

export default function ItemFormPage() {
  const { id } = useParams();
  const isEdit = !!id;
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<ItemForm>(defaultForm);

  const { data } = useQuery({
    queryKey: ["item", id],
    queryFn: async () => {
      const res = await api.get(`/api/v1/master-data/items/${id}`);
      return res.data.data;
    },
    enabled: isEdit,
  });

  useEffect(() => {
    if (data) {
      setForm({
        code: data.code,
        itemType: data.itemType,
        itemGroup: data.itemGroup || "",
        unitOfMeasure: data.unitOfMeasure,
        specification: data.specification || "",
        weight: data.weight?.toString() || "",
        volume: data.volume?.toString() || "",
        makerName: data.makerName || "",
        makerItemNo: data.makerItemNo || "",
        qualityInspectionRequired: data.qualityInspectionRequired,
        phantomBom: data.phantomBom,
        translations: data.translations?.length
          ? data.translations
          : [{ locale: "ko", name: "", description: "" }],
      });
    }
  }, [data]);

  const saveMutation = useMutation({
    mutationFn: async () => {
      const body = {
        ...form,
        weight: form.weight ? parseFloat(form.weight) : null,
        volume: form.volume ? parseFloat(form.volume) : null,
      };
      if (isEdit) {
        return api.put(`/api/v1/master-data/items/${id}`, body);
      }
      return api.post("/api/v1/master-data/items", body);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["items"] });
      navigate("/master-data/items");
    },
  });

  const set = (field: keyof ItemForm, value: unknown) =>
    setForm((prev) => ({ ...prev, [field]: value }));

  const setTranslation = (index: number, field: string, value: string) => {
    const updated = [...form.translations];
    updated[index] = { ...updated[index], [field]: value };
    setForm((prev) => ({ ...prev, translations: updated }));
  };

  return (
    <div>
      <PageHeader
        title={isEdit ? `Edit Item — ${form.code}` : "New Item"}
        breadcrumbs={[
          { label: "Master Data", path: "/master-data" },
          { label: "Items", path: "/master-data/items" },
          { label: isEdit ? form.code : "New" },
        ]}
        actions={
          <>
            <button className="btn-secondary" onClick={() => navigate("/master-data/items")}>
              <ArrowLeft size={16} /> Back
            </button>
            {isEdit && (
              <button className="btn-danger"><Trash2 size={16} /> Delete</button>
            )}
            <button className="btn-primary" onClick={() => saveMutation.mutate()}
              disabled={saveMutation.isPending}>
              <Save size={16} /> {saveMutation.isPending ? "Saving..." : "Save"}
            </button>
          </>
        }
      />

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Basic Info */}
        <div className="card p-6 lg:col-span-2">
          <h3 className="text-base font-semibold text-slate-900 mb-5">Basic Information</h3>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">Item Code *</label>
              <input className="input font-mono" value={form.code} disabled={isEdit}
                onChange={(e) => set("code", e.target.value)} placeholder="ITEM-001" />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">Type *</label>
              <select className="input" value={form.itemType} onChange={(e) => set("itemType", e.target.value)}>
                <option value="MATERIAL">Material</option>
                <option value="PRODUCT">Product</option>
                <option value="SEMI_PRODUCT">Semi Product</option>
                <option value="SERVICE">Service</option>
                <option value="ASSET">Asset</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">Group</label>
              <input className="input" value={form.itemGroup} onChange={(e) => set("itemGroup", e.target.value)} />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">Unit of Measure</label>
              <select className="input" value={form.unitOfMeasure} onChange={(e) => set("unitOfMeasure", e.target.value)}>
                {["EA", "KG", "L", "M", "M2", "M3", "SET", "BOX", "ROLL"].map((u) => (
                  <option key={u} value={u}>{u}</option>
                ))}
              </select>
            </div>
            <div className="sm:col-span-2">
              <label className="block text-sm font-medium text-slate-700 mb-1.5">Specification</label>
              <input className="input" value={form.specification} onChange={(e) => set("specification", e.target.value)} />
            </div>
          </div>
        </div>

        {/* Properties */}
        <div className="space-y-6">
          <div className="card p-6">
            <h3 className="text-base font-semibold text-slate-900 mb-5">Properties</h3>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">Weight (kg)</label>
                <input className="input" type="number" step="0.01" value={form.weight}
                  onChange={(e) => set("weight", e.target.value)} />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">Volume (m³)</label>
                <input className="input" type="number" step="0.01" value={form.volume}
                  onChange={(e) => set("volume", e.target.value)} />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">Maker</label>
                <input className="input" value={form.makerName} onChange={(e) => set("makerName", e.target.value)} />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">Maker Item No.</label>
                <input className="input" value={form.makerItemNo} onChange={(e) => set("makerItemNo", e.target.value)} />
              </div>
            </div>
          </div>

          <div className="card p-6">
            <h3 className="text-base font-semibold text-slate-900 mb-5">Options</h3>
            <div className="space-y-4">
              <label className="flex items-center gap-3 cursor-pointer">
                <input type="checkbox" className="h-4 w-4 rounded border-slate-300 text-brand-600 focus:ring-brand-500"
                  checked={form.qualityInspectionRequired}
                  onChange={(e) => set("qualityInspectionRequired", e.target.checked)} />
                <span className="text-sm text-slate-700">Quality Inspection Required</span>
              </label>
              <label className="flex items-center gap-3 cursor-pointer">
                <input type="checkbox" className="h-4 w-4 rounded border-slate-300 text-brand-600 focus:ring-brand-500"
                  checked={form.phantomBom}
                  onChange={(e) => set("phantomBom", e.target.checked)} />
                <span className="text-sm text-slate-700">Phantom BOM</span>
              </label>
            </div>
          </div>
        </div>

        {/* Translations */}
        <div className="card p-6 lg:col-span-3">
          <h3 className="text-base font-semibold text-slate-900 mb-5">Translations</h3>
          {form.translations.map((t, i) => (
            <div key={i} className="grid grid-cols-1 sm:grid-cols-4 gap-4 mb-4 pb-4 border-b border-slate-100 last:border-0">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">Locale</label>
                <select className="input" value={t.locale} onChange={(e) => setTranslation(i, "locale", e.target.value)}>
                  <option value="ko">Korean</option>
                  <option value="en">English</option>
                  <option value="ja">Japanese</option>
                  <option value="zh">Chinese</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">Name *</label>
                <input className="input" value={t.name} onChange={(e) => setTranslation(i, "name", e.target.value)} />
              </div>
              <div className="sm:col-span-2">
                <label className="block text-sm font-medium text-slate-700 mb-1.5">Description</label>
                <input className="input" value={t.description} onChange={(e) => setTranslation(i, "description", e.target.value)} />
              </div>
            </div>
          ))}
          <button className="btn-ghost text-sm"
            onClick={() => setForm({ ...form, translations: [...form.translations, { locale: "en", name: "", description: "" }] })}>
            + Add Translation
          </button>
        </div>
      </div>
    </div>
  );
}
