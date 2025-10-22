import React, { useState, useEffect } from 'react';
import { View, Text, StyleSheet, ScrollView, Pressable, Image, ActivityIndicator, Alert } from 'react-native';
import TopBar from '../components/TopBar';
import { SCALE } from '../utils/constants';
import { PieChart } from '../components/PieChart';
import { fetchEcoReports, fetchCurrentMonthReport, EcoReport as ApiEcoReport } from '../utils/ecoReportApi';

// API EcoReport 타입을 직접 사용
export type EcoReport = ApiEcoReport;

interface EcoReportScreenProps {
  onBack?: () => void;
  onHome?: () => void;
  onOpenDetail: (report: EcoReport) => void;
}

export default function EcoReportScreen({ onBack, onHome, onOpenDetail }: EcoReportScreenProps) {
  const [reports, setReports] = useState<EcoReport[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadReports();
  }, []);

  const loadReports = async () => {
    try {
      setLoading(true);
      
      // 기존 리포트 조회만 시도
      const apiReports = await fetchEcoReports();
      console.log('🔍 EcoReportScreen - API에서 받은 리포트 데이터:');
      console.log('  - 리포트 개수:', apiReports.length);
      
      if (apiReports.length > 0) {
        apiReports.forEach((report, index) => {
          console.log(`🔍 EcoReportScreen - 리포트 #${index + 1}:`, {
            reportMonth: report.reportMonth,
            activitiesCount: report.activities.length,
            activities: report.activities.map(a => ({ label: a.label, count: a.count, points: a.points }))
          });
        });
      }
      
      setReports(apiReports);
    } catch (err) {
      console.error('리포트 로딩 실패:', err);
      // 에러가 발생해도 빈 배열로 설정하여 빈 화면 표시
      setReports([]);
    } finally {
      setLoading(false);
    }
  };

  // API 데이터를 직접 사용하므로 변환 함수 제거
  const safeNumber = (value: number | undefined | null, fallback = 0): number => {
    return isNaN(value as number) || value == null ? fallback : (value as number);
  };

  if (loading) {
    return (
      <View style={styles.container}>
        <TopBar title="친환경 리포트" onBack={onBack} onHome={onHome} />
        <View style={styles.loadingContainer}>
          <ActivityIndicator size="large" color="#10B981" />
          <Text style={styles.loadingText}>리포트를 불러오는 중...</Text>
        </View>
      </View>
    );
  }



  return (
    <View style={styles.container}>
      <TopBar title="친환경 리포트" onBack={onBack} onHome={onHome} />
      <ScrollView style={styles.content} showsVerticalScrollIndicator={false}>
        {/* 리포트 설명 섹션 */}
        <Text style={styles.descriptionTitle}>월별 친환경 활동 리포트</Text>
        <Text style={styles.descriptionSubtitle}>지난달 나의 작은 실천이 만든 큰 변화를 확인해보세요.{'\n'}지구와 나 모두에게 이로운 혜택이 쌓이고 있어요.</Text>
        <View style={styles.trophyIcon}>
          <Image 
            source={require('../../assets/report.png')} 
            style={styles.trophyImage}
            resizeMode="contain"
          />
        </View>

        {/* 리포트 목록 */}
        <View>
          {reports.length > 0 ? (
            <View style={styles.menuList}>
              {reports.map((report, idx) => (
                <Pressable 
                  key={report.reportId} 
                  style={[styles.menuItem, idx === reports.length - 1 && styles.lastMenuItem]} 
                  onPress={() => onOpenDetail(report)}
                >
                  <View style={styles.menuItemLeft}>
                    <View>
                      <View style={styles.titleContainer}>
                        <Text style={styles.menuTitle}>{report.reportMonth}</Text>
                        {idx === 0 && (
                          <View style={styles.newBadge}>
                            <Text style={styles.newBadgeText}>NEW</Text>
                          </View>
                        )}
                      </View>
                      <Text style={styles.menuSubtitle}>
                        씨앗 {safeNumber(report.statistics.totalSeeds).toLocaleString()}개 · 탄소 {safeNumber(report.statistics.totalCarbonKg)}kg 절감
                      </Text>
                      <Text style={styles.menuLevel}>
                        {report.summary.currentLevel} · 진행률 {safeNumber(report.summary.levelProgress)}%
                      </Text>
                    </View>
                  </View>
                  <Text style={styles.chevron}>›</Text>
                </Pressable>
              ))}
            </View>
          ) : (
            <View style={styles.emptyListContainer}>
              <Text style={styles.emptyListText}>아직 친환경 활동 리포트가 없어요</Text>
              <Text style={styles.emptyListSubText}>
                걷기, 챌린지, 퀴즈 등 다양한 친환경 활동을{'\n'}
                진행해보세요! 활동하시면 리포트가 생성됩니다.
              </Text>
            </View>
          )}
        </View>

        <View style={{ height: 60 * SCALE }} />
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#FFFFFF' },
  content: { flex: 1, padding: 20 * SCALE },
  // card: { backgroundColor: '#FFFFFF', borderRadius: 16 * SCALE, padding: 16 * SCALE, borderWidth: 1, borderColor: '#E5E7EB', marginBottom: 16 * SCALE },
  cardTitle: { fontSize: 16 * SCALE, fontWeight: '800', color: '#111827', marginBottom: 12 * SCALE },
  
  // 설명 섹션 스타일
  descriptionTitle: { 
    fontSize: 24 * SCALE, 
    fontWeight: '800', 
    color: '#111827', 
    textAlign: 'left',
    marginVertical: 12 * SCALE,
    marginLeft: 8 * SCALE
  },
  descriptionSubtitle: { 
    fontSize: 14 * SCALE, 
    color: '#6B7280', 
    textAlign: 'left',
    lineHeight: 20 * SCALE,
    marginBottom: 16 * SCALE,
    marginLeft: 8 * SCALE
  },
  trophyIcon: { 
    alignItems: 'center', 
    justifyContent: 'center' 
  },
  trophyImage: { 
    width: 250 * SCALE, 
    height: 220 * SCALE, 
  },
  
  menuList: { backgroundColor: '#FFFFFF', borderRadius: 12 * SCALE, overflow: 'hidden', borderWidth: 1, borderColor: '#F3F4F6' },
  menuItem: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: 16 * SCALE, paddingVertical: 14 * SCALE, borderBottomWidth: 1, borderBottomColor: '#F3F4F6' },
  lastMenuItem: { borderBottomWidth: 0 },
  menuItemLeft: { flexDirection: 'row', alignItems: 'center', gap: 12 * SCALE },
  menuIcon: { width: 40 * SCALE, height: 40 * SCALE, borderRadius: 8 * SCALE, backgroundColor: '#F3F4F6', alignItems: 'center', justifyContent: 'center' },
  menuIconText: { fontSize: 12 * SCALE, color: '#374151', fontWeight: '800' },
  titleContainer: { 
    flexDirection: 'row', 
    alignItems: 'center', 
    marginBottom: 8 * SCALE,
    gap: 8 * SCALE
  },
  menuTitle: { fontSize: 18 * SCALE, color: '#111827', fontWeight: '700' },
  newBadge: { 
    backgroundColor: '#EF4444', 
    paddingHorizontal: 6 * SCALE, 
    paddingVertical: 2 * SCALE, 
    borderRadius: 4 * SCALE 
  },
  newBadgeText: { 
    fontSize: 10 * SCALE, 
    color: '#FFFFFF', 
    fontWeight: '800' 
  },
  menuSubtitle: { fontSize: 14 * SCALE, color: '#6B7280' },
  menuLevel: { fontSize: 12 * SCALE, color: '#198376', fontWeight: '600', marginTop: 4 * SCALE },
  chevron: { fontSize: 22 * SCALE, color: '#D1D5DB' },
  
  // 로딩 및 에러 상태 스타일
  loadingContainer: { 
    flex: 1, 
    justifyContent: 'center', 
    alignItems: 'center', 
    padding: 40 * SCALE 
  },
  loadingText: { 
    fontSize: 16 * SCALE, 
    color: '#6B7280', 
    marginTop: 16 * SCALE 
  },
  emptyListContainer: { 
    backgroundColor: '#FFFFFF', 
    borderRadius: 16 * SCALE, 
    padding: 32 * SCALE,
    alignItems: 'center',
    borderWidth: 1, 
    borderColor: '#F3F4F6'
  },
  emptyListIconContainer: {
    marginBottom: 20 * SCALE
  },
  emptyListIcon: {
    fontSize: 48 * SCALE,
    textAlign: 'center'
  },
  emptyListText: { 
    fontSize: 18 * SCALE, 
    color: '#374151', 
    fontWeight: '600', 
    marginBottom: 8 * SCALE,
    textAlign: 'center'
  },
  emptyListSubText: { 
    fontSize: 14 * SCALE, 
    color: '#6B7280',
    marginBottom: 24 * SCALE,
    textAlign: 'center',
    lineHeight: 20 * SCALE
  },
  activitySuggestions: {
    backgroundColor: '#F9FAFB',
    borderRadius: 16 * SCALE,
    padding: 20 * SCALE,
    width: '100%',
    maxWidth: 320 * SCALE
  },
  suggestionTitle: {
    fontSize: 16 * SCALE,
    fontWeight: '600',
    color: '#374151',
    marginBottom: 16 * SCALE,
    textAlign: 'center'
  },
  suggestionList: {
    gap: 12 * SCALE
  },
  suggestionItem: {
    fontSize: 14 * SCALE,
    color: '#6B7280',
    textAlign: 'center',
    lineHeight: 20 * SCALE
  },
});


